// IGroupCallback.aidl
package inx.sethway;

// Declare any non-default types here with import statements

interface IGroupCallback {

    // A new peer joined the group, through our invite
    void onNewPeerConnected(String commonInfo);

    // We (the invitee) have successfully joined the group
    void onGroupJoinSuccess();
}