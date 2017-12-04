import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}

import scala.io.Source
import scala.util.Try

object MainBot extends TelegramBot with Polling with Commands {
  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
  lazy val token = scala.util.Properties
    .envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  onCommand('start) { implicit msg => reply(  "Bueno cabros se viene la hora de pagar. \n")}

  onCommand('help) { implicit msg => reply("Usar los comandos para agregar las deudas pendientes. \n" +
                                            "Agregar deuda con commando /ledebo o /medebe @tagdeudor monto\n" +
                                            "Saldar deuda o ya pago usar commando /mepago @tagdeudor monto\n" +
                                            "Mostrar deudas pendientes del usuario usar commando /misdeudas\n" +
                                            "Para ver mis deudores /paguenctm")}

  onCommand('medebe) { implicit msg =>
    withArgs { args =>
      val user = msg.from.get.username.get
      val value = args.last
      println(Try(value.toInt))
      val tags = args.takeWhile(_.startsWith("@"))
      if (tags.length <= 0)
        reply("No tiene tags m3n")
      else {
        val message = args.slice(tags.length, args.length - 1).mkString(" ")
//        DBInterface.addMultipleDebt(tags.toList, user, value.toInt, "")
        reply("Ack")
      }
    }
  }

  onCommand('ledebo) { implicit msg =>
    withArgs { args =>
      val user = msg.from.get.username.get
      val value = args.last
      val tags = args.takeWhile(_.startsWith("@"))

      if (tags.length > 1 || tags.length <= 0) {
        reply("No puedes realizar esta operación m3n")
      }

      else
        {
          val message = args.slice(tags.length, args.length - 1).mkString(" ")
          DBInterface.addSingleDebt(user, tags.head, value.toInt, "")
          reply("Ack")
        }
    }
  }
}