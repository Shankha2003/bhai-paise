import { createHash } from "node:crypto";
import { getAuth } from "firebase-admin/auth";
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";

initializeApp();

type MemberProfile = {
  name?: string;
  userUid?: string;
};

export const notifyMembersOnGroupJoin = onDocumentUpdated(
  {
    document: "sharedGroups/{groupId}",
    region: "asia-south1",
    retry: true,
    maxInstances: 3,
  },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const oldUids = new Set<string>(Array.isArray(before.memberUids) ? before.memberUids : []);
    const newUids = Array.isArray(after.memberUids) ? after.memberUids as string[] : [];
    const joiningUids = newUids.filter((uid) => typeof uid === "string" && !oldUids.has(uid));
    if (joiningUids.length === 0) return;

    const groupId = event.params.groupId;
    const groupName = typeof after.name === "string" && after.name.trim() ? after.name.trim() : "your group";
    const profiles = (after.members ?? {}) as Record<string, MemberProfile>;
    const db = getFirestore();

    for (const joiningUid of joiningUids) {
      const joiningProfile = Object.values(profiles).find((profile) => profile.userUid === joiningUid);
      const joiningName = joiningProfile?.name?.trim() || "A member";
      const recipients = [...oldUids].filter((uid) => uid !== joiningUid);

      for (const recipientUid of recipients) {
        const emailRef = db.collection("mail").doc(
          createHash("sha256").update(`${event.id}:${joiningUid}:${recipientUid}`).digest("hex"),
        );

        const recipient = await getAuth().getUser(recipientUid).catch(() => null);
        const recipientEmail = recipient?.emailVerified ? recipient.email : undefined;
        if (recipientEmail) {
          await emailRef.create({
            to: recipientEmail,
            message: {
              subject: `${joiningName} joined ${groupName}`,
              text: `${joiningName} has joined your group “${groupName}” on Bhai Paise. Open the app to see the group and its latest bills.`,
            },
            createdAt: FieldValue.serverTimestamp(),
          }).catch((error: { code?: number | string }) => {
            if (error.code !== 6 && error.code !== "already-exists") throw error;
          });
        }

        const tokenCollection = db.collection("userDevices").doc(recipientUid).collection("tokens");
        const tokenDocs = await tokenCollection.get();
        const tokens = tokenDocs.docs.map((doc) => doc.id);
        for (let offset = 0; offset < tokens.length; offset += 500) {
          const chunk = tokens.slice(offset, offset + 500);
          if (chunk.length === 0) continue;
          const result = await getMessaging().sendEachForMulticast({
            tokens: chunk,
            notification: {
              title: "New group member",
              body: `${joiningName} joined ${groupName}`,
            },
            data: { groupId, kind: "group_member_joined" },
            android: { priority: "high" },
          });
          await Promise.all(result.responses.map((response, index) => {
            if (response.success) return Promise.resolve();
            const code = response.error?.code;
            if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
              return tokenCollection.doc(chunk[index]).delete();
            }
            return Promise.resolve();
          }));
        }
      }
    }
  },
);
