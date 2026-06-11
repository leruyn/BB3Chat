import * as admin from "firebase-admin";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";

admin.initializeApp();

/**
 * Data-only FCM when a room message is created.
 * Recipients = room.memberUids minus message.senderUid.
 */
export const onNewMessage = onDocumentCreated(
  "rooms/{roomId}/messages/{msgId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const roomId = event.params.roomId as string;
    const msg = snap.data();
    const senderUid = msg.senderUid as string | undefined;

    const roomSnap = await admin.firestore().doc(`rooms/${roomId}`).get();
    if (!roomSnap.exists) return;

    const memberUids: string[] = roomSnap.data()?.memberUids ?? [];
    const recipients = memberUids.filter((uid) => uid && uid !== senderUid);
    if (recipients.length === 0) return;

    const tokenSnaps = await Promise.all(
      recipients.map((uid) => admin.firestore().doc(`fcm_tokens/${uid}`).get())
    );

    const tokens = tokenSnaps
      .filter((doc) => doc.exists && typeof doc.data()?.token === "string")
      .map((doc) => doc.data()!.token as string);

    if (tokens.length === 0) {
      logger.info("onNewMessage: no FCM tokens for recipients", { roomId, recipients });
      return;
    }

    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      data: {
        action: "NEW_MESSAGE",
        roomId,
      },
      android: {
        priority: "high",
      },
      apns: {
        headers: {
          "apns-push-type": "background",
          "apns-priority": "5",
        },
        payload: {
          aps: {
            "content-available": 1,
          },
        },
      },
    });

    logger.info("onNewMessage: FCM sent", {
      roomId,
      success: response.successCount,
      failure: response.failureCount,
    });
  }
);
