import scalikejdbc._

object DBInterface {

    Class.forName("org.sqlite.JDBC")
    ConnectionPool.singleton("jdbc:sqlite:moroso.db", null, null)

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
        enabled = true,
        singleLineMode = true,
        logLevel = 'info
    )

    implicit val session = AutoSession

    case class Debt(user_from: String, user_to: String, reason: String, amount: Int)

    def existsUser(user: String): Boolean = {
        sql"SELECT count(*) AS c FROM telegram_user WHERE tag = $user"
            .map(res => res.int("c"))
            .single().apply().getOrElse(0) > 0
    }

    def addUser(user: String): Unit = {
        sql"INSERT INTO telegram_user (tag) VALUES ($user)"
            .update().apply()
    }

    private def getUserId(user: String): Int = {
        if (!existsUser(user))
            addUser(user)
        sql"SELECT id FROM telegram_user WHERE tag = $user"
            .map(res => res.int("id"))
            .single().apply().getOrElse(-1)
    }

    private def addReason(msg: String): Int = {
        sql"INSERT INTO reason(message) VALUES ($msg)"
            .update().apply()
        sql"SELECT last_insert_rowid() AS id"
            .map(res => res.int("id"))
            .single().apply().getOrElse(-1)
    }

    def addSingleDebt(userFrom: String, userTo: String, amount: Int, reason: String): Unit = {
        if (amount <= 0) return
        val idReason = addReason(reason)
        val idFrom = getUserId(userFrom)
        val idTo = getUserId(userTo)
        sql"INSERT INTO debt(user_from, user_to, amount, indebted_amount, msg_id) VALUES ($idFrom, $idTo, $amount, $amount, $idReason)"
            .update().apply()
    }

    def addMultipleDebt(usersFrom: List[String], userTo: String, amount: Int, reason: String): Unit = {
        if (amount <= 0) return
        val idReason = addReason(reason)
        val idTo = getUserId(userTo)
        usersFrom.foreach { userFrom =>
            val idFrom = getUserId(userFrom)
            sql"INSERT INTO debt(user_from, user_to, amount, indebted_amount, msg_id) VALUES ($idFrom, $idTo, $amount, $amount, $idReason)"
                .update().apply()
        }
    }

    def getUserDebts(user: String): List[Debt] = {
        val idUser = getUserId(user)
        sql"SELECT u.tag, d.indebted_amount, r.message FROM debt AS d LEFT JOIN telegram_user AS u ON d.user_to = u.id LEFT OUTER JOIN reason AS r ON r.id = d.msg_id WHERE d.active = 1 AND d.user_from = $idUser"
            .map(res => Debt(user, res.string("tag"), res.string("message"), res.int("indebted_amount")))
            .list().apply()
    }

    def getUserIncomes(user: String): List[Debt] = {
        val idUser = getUserId(user)
        sql"SELECT u.tag, d.indebted_amount, r.message FROM debt AS d LEFT JOIN telegram_user AS u ON d.user_from = u.id LEFT OUTER JOIN reason AS r ON r.id = d.msg_id WHERE d.active = 1 AND d.user_to = $idUser"
            .map(res => Debt(res.string("tag"), user, res.string("message"), res.int("indebted_amount")))
            .list().apply()
    }

    def getAggregatedDebts: List[Debt] = {
        sql"SELECT uf.tag AS uf, ut.tag AS ut, a.amount FROM active_debt AS a INNER JOIN telegram_user AS uf ON a.user_from = uf.id INNER JOIN telegram_user AS ut ON a.user_to = ut.id"
            .map(res => Debt(res.string("uf"), res.string("ut"), "", res.int("amount")))
            .list().apply()
    }

    private def updateDebt(debtId: Int, indebted: Int): Unit = {
        val active = if (indebted > 0) 1 else 0
        sql"UPDATE debt SET indebted_amount = $indebted, active = $active WHERE id = $debtId"
            .update().apply()
    }

    def addPayment(userFrom: String, userTo: String, amount: Int): Boolean = {
        val idFrom = getUserId(userFrom)
        val idTo = getUserId(userTo)
        var remainder = amount
        var updates = List[(Int, Int)]()

        sql"SELECT id, indebted_amount FROM debt WHERE active = 1 AND user_to = $idTo AND user_from = $idFrom ORDER BY creation_time ASC"
            .foreach { res =>
                if (remainder > 0) {
                    val debtAmount = res.int("indebted_amount")
                    val id = res.int("id")
                    if (remainder >= debtAmount) {
                        updates ::= (id, 0)
                        remainder -= debtAmount
                    }
                    else {
                        updates ::= (id, debtAmount - remainder)
                        remainder = 0
                    }
                }
            }

        for ((id, am) <- updates) {
            updateDebt(id, am)
        }

        remainder <= 0
    }

    def isBanned(tag: String): Boolean = {
        if(tag == "") return true
        sql"SELECT count(*) AS c FROM blacklist WHERE tag = $tag"
            .map(res => res.int("c"))
            .single().apply().getOrElse(0) > 0
    }


}
