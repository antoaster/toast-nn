package antoaster.toastnn

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class EvonnDisplay : ApplicationAdapter() {

    companion object {
        val WIDTH = 800
        val HEIGHT = 600

        fun color(r: Double, g: Double, b: Double) = Color(r.toFloat(), g.toFloat(), b.toFloat(), 1.0f)

    }

    private lateinit var context: EvonnContext
    private lateinit var network: NetworkViz

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var batch: SpriteBatch

    private var frames = 0
//    private val scale = 40f

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        shapeRenderer = ShapeRenderer()

        Gdx.input.inputProcessor = EvonnInputProcessor(this)

    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f )
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        network.nodes.forEach {
            with(it) {
                shapeRenderer.color = color
                shapeRenderer.circle(x, y, radius)
            }
        }

        network.connections.forEach {
            with(it) {
                //TODO: this doesn't work.
                Gdx.gl.glLineWidth(thickness)
                shapeRenderer.color = color
                shapeRenderer.line(fromX, fromY, toX, toY)
            }

        }

        shapeRenderer.end()

        batch.begin()
        network.nodes.forEach {
            with(it) {
                font.color = Color.GREEN
                font.draw(batch, it.label, x, y)
            }
        }
        batch.end()


    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    fun initialize(initialContext: EvonnContext) {
        val config = LwjglApplicationConfiguration()

        config.samples = 3 // Antialiasing

        update(initialContext)

        config.width = WIDTH
        config.height = HEIGHT

        val application = LwjglApplication(this, config)
    }

    private fun update(context: EvonnContext) {
        this.context = context
        this.network = NetworkViz.create(context.network)
    }

    private class EvonnInputProcessor(val evonnDisplay: EvonnDisplay) : InputAdapter() {
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val networkViz = evonnDisplay.network

            val clickedNode = networkViz.nodes.firstOrNull { it.pointIsInside(screenX, HEIGHT - screenY) }

            clickedNode?.let { println("Clicked $it") } ?: println("($screenX, $screenY) or ${HEIGHT - screenY}")

            return clickedNode != null
        }
    }

}

data class NetworkViz (
        val nodes: List<NodeViz>,
        val connections: List<ConnectionViz>
) {
    companion object {
        fun create(network: Network): NetworkViz {
            val scale = 60f

            val columns = mutableListOf<MutableList<NodeViz>>()

            network.nodes.forEachIndexed { index, node ->
                val depth = network.maxDepth(index)

                while (columns.size < depth + 1) {
                    columns += mutableListOf<NodeViz>()
                }
                val column = columns[depth]
                val columnPosition = column.size
                val x = scale * (1 + depth)
                val y = scale * (1 + columnPosition)
                val radius = scale / 4
                val color = when (node.gene.nodeType) {
                    NodeType.INPUT -> Color.CORAL
                    NodeType.OUTPUT -> Color.MAGENTA
                    NodeType.HIDDEN -> Color.WHITE
                }
                val label = "$index"
                column.add(NodeViz(x, y, radius, color, label, node))
            }

            val nodes = columns.flatten()

            val connections = network.connections.map { connection ->

                // they don't necessarily have the same index after sorting into columns, so we gotta go find them.
                val fromNodeViz = nodes.find { it.node == connection.fromNode} ?: throw RuntimeException("kaboom")
                val toNodeViz = nodes.find { it.node == connection.toNode} ?: throw RuntimeException("kaboom")
                val thickness = 2 * connection.weight
                val color = if(connection.enabled) Color.MAROON else Color.RED

//                println("Conn ${connection.gene.fromIndex} to ${connection.gene.toIndex} from $fromNodeViz to $toNodeViz")

                ConnectionViz(
                        fromX = fromNodeViz.x,
                        fromY = fromNodeViz.y,
                        toX = toNodeViz.x,
                        toY = toNodeViz.y,
                        thickness = thickness,
                        color = color,
                        connection = connection
                )
            }

            return NetworkViz(nodes, connections)
        }
    }
}

data class NodeViz(
        val x: Float,
        val y: Float,
        val radius: Float,
        val color: Color,
        val label: String,
        val node: Node // TODO: don't use this...
) {
    // TODO: generify this into some kind of bounding box
    fun pointIsInside(screenX: Int, screenY: Int): Boolean {
        if (screenX < x - radius
                || screenX > x + radius
                || screenY < y - radius
                || screenY > y + radius
                )
            return false
        val dx = screenX - x
        val dy = screenY - y

        val d2 = dx * dx + dy * dy
        val r2 = radius * radius
        return d2 <= r2
    }
}

data class ConnectionViz(
        val fromX: Float,
        val fromY: Float,
        val toX: Float,
        val toY: Float,
        val thickness: Float,
        val color: Color,
        val connection: Connection
)
