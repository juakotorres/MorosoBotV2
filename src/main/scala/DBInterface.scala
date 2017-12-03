import scalikejdbc._

object DBInterface {

    Class.forName("org.sqlite.JDBC")
    ConnectionPool.singleton("jdbc:sqlite:moroso.db", null, null)

    implicit val session = AutoSession

    def existsUser(user: String): Boolean = {
        sql"SELECT count(*) AS c FROM telegram_user WHERE tag = $user"
            .map(res => res.int("c"))
            .single().apply().getOrElse(0) > 0
    }

    def addUser(user: String): Unit = {
        sql"INSERT INTO telegram_user (tag) VALUES ($user)"
            .update().apply()
    }

    private def getUserId(user: String): Option[Int] = {
        sql"SELECT id FROM telegram_user WHERE tag = $user"
            .map(res => res.int("id"))
            .single().apply()
    }

    private def addReason(msg: String): Int = {
        sql"INSERT INTO reason(message) VALUES ($msg)"
            .update().apply()
        sql"SELECT last_insert_rowid() AS id"
            .map(res => res.int("id"))
            .single().apply().getOrElse(-1)
    }

    def addSingleDebt(userFrom: String, userTo: String, amount: Int, reason: String): Unit = {
        val idReason = addReason(reason)
        val idFrom = getUserId(userFrom)
        val idTo = getUserId(userTo)
        sql"INSERT INTO debt(user_from, user_to, amount, indebted_amount, msg_id) VALUES ($idFrom, $idTo, $amount, $amount, $idReason)"
            .update().apply()
    }

}
