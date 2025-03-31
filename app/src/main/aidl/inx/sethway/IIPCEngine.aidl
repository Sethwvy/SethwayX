// IIPCEngine.aidl
package inx.sethway;

// Declare any non-default types here with import statements
import inx.sethway.IGroupCallback;

interface IIPCEngine {

    int getPid();

    void createGroup(String groupId);

    // We (inviter) are awaiting for a new peer to join
    // after they scan our QR Code
    void receiveInvitee();

    // Content of the QR Code
    String getInvite();

    // We (invitee) have scanned the QR Code
    void acceptInvite(in byte[] invitation);

    void registerGroupCallback(in IGroupCallback callback);
    void unregisterGroupCallback();
}