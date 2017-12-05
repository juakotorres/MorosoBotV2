import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.ParseMode

import scala.io.Source
import scala.util.Try

object MainBot extends TelegramBot with Polling with Commands {
    lazy val token: String = scala.util.Properties
        .envOrNone("BOT_TOKEN")
        .getOrElse(Source.fromFile("bot.token").getLines().mkString)

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
        var builder = ""
        var sum = 0
        msg.from.foreach { user =>
            builder += s"Deudas de @${user.username.getOrElse("")}\n"
            DBInterface.getUserDebts(user.username.getOrElse("")).foreach { debt =>
                builder += s"*${debt.amount}* a @${debt.user_to} - ${debt.reason}\n"
                sum += debt.amount
            }
            builder += s"Total: *$sum*\n"
        }
        if (sum == 0)
            builder += "No hay deudas m3n"
        reply(builder, Some(ParseMode.Markdown))
    }

    onCommand('paguenctm) { implicit msg =>
        var builder = ""
        var sum = 0
        msg.from.foreach { user =>
            builder += s"Deudas a @${user.username.getOrElse("")}\n"
            DBInterface.getUserIncomes(user.username.getOrElse("")).foreach { debt =>
                builder += s"*${debt.amount}* de @${debt.user_from} - ${debt.reason}\n"
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
            val user = msg.from.get.username.get
            val value = args.last
            if(value.forall(c => c.isDigit)) {
                val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
                if (tags.length <= 0)
                    reply("No tiene tags m3n")
                else {
                    val message = args.slice(tags.length, args.length - 1).mkString(" ")
                    DBInterface.addMultipleDebt(tags.toList, user, value.toInt, message)
                    reply("Oc")
                }
            }
            else{
                reply("Monto no válido m3n")
            }
        }
    }

    onCommand('ledebo) { implicit msg =>
        withArgs { args =>
            val user = msg.from.get.username.get
            val value = args.last
            val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
            if (tags.length > 1 || tags.length <= 0) {
                reply("No puedes realizar esta operación m3n")
            }
            else if(value.forall(c => c.isDigit)){
                val message = args.slice(tags.length, args.length - 1).mkString(" ")
                DBInterface.addSingleDebt(user, tags.head, value.toInt, message)
                reply("Oc")
            }
            else{
                reply("Monto inválido m3n")
            }
        }
    }

    onCommand('mepago) { implicit msg =>
        withArgs { args =>
            val user = msg.from.get.username.get
            val value = args.last
            val tags = args.takeWhile(_.startsWith("@")).map(s => s.substring(1))
            if (tags.length > 1 || tags.length <= 0) {
                reply("No puedes realizar esta operación m3n")
            }
            else if(value.forall(c => c.isDigit)){
                val res = DBInterface.addPayment(tags.head, user, value.toInt)
                if(res)
                    reply("Oc")
                else
                    reply("Estás manqueando m3n, no tienes deuda o pagaste extra")
            }
            else{
                reply("Monto inválido m3n")
            }
        }
    }

}