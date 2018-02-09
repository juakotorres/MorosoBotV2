import java.nio.file.Paths
import java.text.SimpleDateFormat

import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{ParseMode, SendPhoto}
import info.mukel.telegrambot4s.models.{InputFile, Message}

import scala.io.Source
import scala.util.Random

object MainBot extends TelegramBot with Polling with Commands {
    lazy val token: String = scala.util.Properties
        .envOrNone("BOT_TOKEN")
        .getOrElse(Source.fromFile("bot.token").getLines().mkString)

    private def logCommand(msg: Message): Unit = {
        val date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(msg.date * 1000L)
        val txt = msg.text.getOrElse("ERROR: NO TEXT")

        val from = msg.from
        if (from.isEmpty) {
            println(Console.YELLOW + s"[$date]" + Console.RED + " |  ERROR: NO FROM | " + Console.WHITE + txt)
            return
        }
        val username = from.get.username
        if (username.isEmpty) {
            println(Console.YELLOW + s"[$date]" + Console.RED + " | ERROR: NO USERNAME | " + Console.WHITE + txt)
            return
        }
        val user = username.get

        println(Console.YELLOW + s"[$date] " + Console.BLUE + s"@$user: " + Console.WHITE + txt)
    }

    private def escapeMarkdown(s: String): String = {
        s.replace("_","\\_").replace("*","\\*")
    }

    private var graphHasChanged = true

    onCommand('start) { implicit msg =>
        reply("Bueno cabros se viene la hora de pagar. \n")
    }

    onCommand('help) { implicit msg =>
        reply("Usar los comandos para agregar las deudas pendientes. \n" +
            "Agregar deuda con commando /ledebo o /medebe @tagdeudor monto\n" +
            "Saldar deuda o ya pago usar commando /mepago @tagdeudor monto\n" +
            "Mostrar deudas pendientes del usuario usar commando /misdeudas\n" +
            "Para ver mis deudores /paguenctm")
    }

    onCommand('misdeudas) { implicit msg =>
        logCommand(msg)
        var builder = ""
        var sum = 0
        msg.from.foreach { user =>
            builder += s"Deudas de @${escapeMarkdown(user.username.getOrElse(""))}\n"
            DBInterface.getUserDebts(user.username.getOrElse("")).foreach { debt =>
                builder += s"*${debt.amount}* a @${escapeMarkdown(debt.user_to)} - ${escapeMarkdown(debt.reason)}\n"
                sum += debt.amount
            }
            builder += s"Total: *$sum*\n"
        }
        if (sum == 0)
            builder += "No hay deudas m3n"
        reply(builder, Some(ParseMode.Markdown))
    }

    onCommand('paguenctm) { implicit msg =>
        logCommand(msg)
        var builder = ""
        var sum = 0
        msg.from.foreach { user =>
            builder += s"Deudas a @${escapeMarkdown(user.username.getOrElse(""))}\n"
            DBInterface.getUserIncomes(user.username.getOrElse("")).foreach { debt =>
                builder += s"*${debt.amount}* de @${escapeMarkdown(debt.user_from)} - ${escapeMarkdown(debt.reason)}\n"
                sum += debt.amount
            }
            builder += s"Total: *$sum*\n"
        }
        if (sum == 0)
            builder += "No hay money m3n"
        reply(builder, Some(ParseMode.Markdown))
    }

    onCommand('medebe) { implicit msg =>
        withArgs { args =>
            logCommand(msg)
            val user = msg.from.get.username.get
            val value = args.last
            if (value.forall(c => c.isDigit)) {
                val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
                if (tags.length <= 0)
                    reply("No tiene tags m3n")
                else {
                    val message = args.slice(tags.length, args.length - 1).mkString(" ")
                    DBInterface.addMultipleDebt(tags.toList, user, value.toInt, message)
                    graphHasChanged = true
                    reply("Oc")
                }
            }
            else {
                reply("Monto no válido m3n")
            }
        }
    }

    onCommand('ledebo) { implicit msg =>
        withArgs { args =>
            logCommand(msg)
            val user = msg.from.get.username.get
            val value = args.last
            val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
            if (tags.length > 1 || tags.length <= 0) {
                reply("No puedes realizar esta operación m3n")
            }
            else if (value.forall(c => c.isDigit)) {
                val message = args.slice(tags.length, args.length - 1).mkString(" ")
                DBInterface.addSingleDebt(user, tags.head, value.toInt, message)
                graphHasChanged = true
                reply("Oc")
            }
            else {
                reply("Monto inválido m3n")
            }
        }
    }

    onCommand('mepago) { implicit msg =>
        withArgs { args =>
            logCommand(msg)
            val user = msg.from.get.username.get
            val value = args.last
            val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
            if (tags.length > 1 || tags.length <= 0) {
                reply("No puedes realizar esta operación m3n")
            }
            else if (value.forall(c => c.isDigit)) {
                val res = DBInterface.addPayment(tags.head, user, value.toInt)
                graphHasChanged = true
                if (res)
                    reply("Oc")
                else
                    reply("Estás manqueando m3n, no tienes deuda o pagaste extra")
            }
            else {
                reply("Monto inválido m3n")
            }
        }
    }

    onCommand('all) { implicit msg =>
        logCommand(msg)
        if(graphHasChanged) {
            val debts = DBInterface.getAggregatedDebts
            Graph.restart()
            Graph.addDebts(debts)
            Graph.draw()
            graphHasChanged = false
        }
        request(SendPhoto(msg.source, InputFile(Paths.get("grafo.png"))))
    }

    onCommand('otp) { implicit msg =>
        val names = Array("Pelao", "Huan", "Juaki", "Gabriel", "Beli", "Americo", "Sergio", "Jaev", "Rodrigo")
        val myst = Random.shuffle(names.toList)
        reply(s"/${myst.head}X${myst(1)}")
    }

}