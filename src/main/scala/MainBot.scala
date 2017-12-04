import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Polling, TelegramBot}

import scala.io.Source

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

}