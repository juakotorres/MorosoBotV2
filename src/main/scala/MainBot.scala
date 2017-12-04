import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.ParseMode

import scala.io.Source

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
        if(sum == 0)
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
        if(sum == 0)
            builder += "No hay money m3n"
        reply(builder, Some(ParseMode.Markdown))
    }

}