package `in`.sethway.engine

interface IIPCEngine {
  fun getPid(): Int

  fun createNewGroup(groupId: String)
  fun joinGroup(groupId: String, creator: String)

  fun getInvite(): String
  fun acceptInvite(invite: String)
}