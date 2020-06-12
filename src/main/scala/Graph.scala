import java.awt.image.BufferedImage
import java.awt.{Graphics2D,Color,Font,BasicStroke}
import java.awt.geom._


object Graph {

  // List with users nodes.
  var nodes : collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer.empty[String]
  // Hashmap with edges debt_from -> debt_to as a list [debt_from, debt_to], value is the total amount of the debt.
  var edges : collection.mutable.HashMap[List[String], Int] = collection.mutable.HashMap.empty[List[String], Int]

  def draw() = {


    val width = 700
    val height = 700
    val radius = 280
    val circleRadius = 25
    var nodesHash : collection.mutable.HashMap[String, List[Double]] = collection.mutable.HashMap.empty[String, List[Double]]
    var colorHash : collection.mutable.HashMap[String, Color] = collection.mutable.HashMap.empty[String, Color]

    // Size of image
    val size = (width, height)
    val xcenter = width / 2
    val ycenter = height / 2

    // create an image
    val canvas = new BufferedImage(size._1, size._2, BufferedImage.TYPE_INT_RGB)

    // get Graphics2D for the image
    val g = canvas.createGraphics()

    // clear background
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, canvas.getWidth, canvas.getHeight)

    // enable anti-aliased rendering (prettier lines and circles)
    // Comment it out to see what this does!
    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
      java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

    // First we need the node positions, then draw de edges, and finally draw de circles.
    val numberOfCircles = nodes.size
    val arcDistance = 2 * Math.PI / numberOfCircles
    var iterator = 0
    val font = new Font("DejaVuSansCondensed", Font.PLAIN, 20)
    g.setFont(font)

    nodes.foreach(node => {
      val xcirle = xcenter + radius*Math.cos(arcDistance*iterator)
      val ycirle = ycenter + radius*Math.sin(arcDistance*iterator)

      nodesHash ++= Map(node -> List(xcirle, ycirle))
      colorHash ++= Map(node -> Color.decode("#4CAF50"))
      iterator = iterator + 1
    })

    edges.foreach(edge => {
      val deudores = edge._1
      val amount = edge._2

      val from = deudores.head
      val to = deudores.tail.head

      colorHash(from) = Color.decode("#F44336")

      val fromCircle : List[Double] = nodesHash(from)
      val toCircle : List[Double] = nodesHash(to)

      g.setStroke(new BasicStroke())
      g.setColor(Color.GRAY)
      g.draw(new Line2D.Double(fromCircle.head, fromCircle.tail.head, toCircle.head, toCircle.tail.head))
      val x0 = fromCircle.head.toInt
      val y0 = fromCircle.tail.head.toInt
      val x1 = toCircle.head.toInt
      val y1 = toCircle.tail.head.toInt
      val magnitude = Math.sqrt(Math.pow(x1-x0,2) + Math.pow(y1-y0,2))
      val xu = (x1 - x0) / magnitude
      val yu = (y1 - y0) / magnitude
      drawArrow(g, x0, y0, x1 - (circleRadius*xu).toInt, y1 - (circleRadius*yu).toInt)

      g.setColor(Color.BLACK)
      g.drawString(amount.toString, ((fromCircle.head + toCircle.head)/2).toInt, ((fromCircle.tail.head + toCircle.tail.head)/2).toInt)
    })

    iterator = 0

    val metrics = g.getFontMetrics(font)

    nodes.foreach(node => {
      val xcirle = xcenter + radius*Math.cos(arcDistance*iterator)
      val ycirle = ycenter + radius*Math.sin(arcDistance*iterator)

      g.setColor(colorHash(node))
      // (x,y, width, height)
      g.fill(new Ellipse2D.Double(xcirle - circleRadius, ycirle - circleRadius, 2*circleRadius, 2*circleRadius))
      g.setColor(Color.BLACK)

      // Get the FontMetrics// Get the FontMetrics
      val stringLen = metrics.getStringBounds(node, g).getCenterX.asInstanceOf[Int]

      g.drawString(node, xcirle.toInt - stringLen, ycirle.toInt + 5)
      iterator = iterator + 1

    })

    // done with drawing
    g.dispose()

    // write image to a file
    javax.imageio.ImageIO.write(canvas, "png", new java.io.File("grafo.png"))
  }

  def addDebts(debts: List[DBInterface.Debt]) = {
    debts.foreach(debt => {
      if (!nodes.contains(debt.user_from))
        nodes += debt.user_from
      if (!nodes.contains(debt.user_to))
        nodes += debt.user_to


      val edgeList = List(debt.user_from, debt.user_to)
      if(edges.contains(edgeList)){
        edges(edgeList) = edges(edgeList) + debt.amount
      } else {
        edges ++= Map(edgeList -> debt.amount)
      }

    })
  }

  // Reinitialization of the graph variables.
  def restart() = {
    nodes = collection.mutable.ListBuffer.empty[String]
    edges = collection.mutable.HashMap.empty[List[String], Int]
  }

  import java.awt.Graphics2D
  import java.awt.geom.AffineTransform

  def drawArrow(g1: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int): Unit = {
    val ARR_SIZE = 10
    val g = g1.create.asInstanceOf[Graphics2D]
    val dx = x2 - x1
    val dy = y2 - y1
    val angle = Math.atan2(dy, dx)
    val len = Math.sqrt(dx * dx + dy * dy).toInt
    val at = AffineTransform.getTranslateInstance(x1, y1)
    at.concatenate(AffineTransform.getRotateInstance(angle))
    g.transform(at)
    // Draw horizontal arrow starting in (0, 0)
    g.drawLine(0, 0, len, 0)
    g.fillPolygon(Array[Int](len, len - ARR_SIZE, len - ARR_SIZE, len), Array[Int](0, -ARR_SIZE, ARR_SIZE, 0), 4)
  }
}
