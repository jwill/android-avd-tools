import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyjarjarcommonscli.DefaultParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Options
import java.io.File
import java.sql.Time
import java.util.*

fun main(args: Array<String>) {
    // Call the converter from IDEA
    //ConvertSVG().renderFile("examples/example.svg")

    // Use from the commandline
    MergeShapeShifter().merge(arrayOf("blue-plane-clean.shapeshifter", "red-circle-clean.shapeshifter"))
    //MergeShapeShifter().merge(args)

}

class MergeShapeShifter() {
    var frameInterval = 5
    var timeInterval = 50

    var startId = 200

    fun merge(filenames: Array<String>) {
        val outputFile = File("combined-clean-55.shapeshifter")

        val groups = arrayListOf<Group>()
        val animationBlocks = arrayListOf<Any>()
        // Create skeleton file
        var width: Int = -1
        var height: Int = -1
        var duration: Int = -1
        for (i in filenames) {
            val file = File(i)
            val f = JsonSlurper().parse(file)
            val json: Map<Any, Any> = f as Map<Any, Any>
            val layers = json["layers"] as Map<Any, Any>
            // parse vector layer
            val vectorLayer = Layer.parse(layers["vectorLayer"] as Map<Any, Any>)

            if (width == -1) {
                width = vectorLayer.width
                height = vectorLayer.height
            }


            // Use filename as group name
            val groupId = vectorLayer.id
            val groupName = file.name.removeSuffix(".shapeshifter").replace("-","_")


            val vectorChildren = vectorLayer.children
            println(vectorChildren.size)


            // Append them into a new group
            var group = Group(
                    id = groupId,
                    name = groupName,
                    children = vectorChildren
            )

            groups.add(group)


            // Grab the animations
            val timeline = json["timeline"] as Map<*, *>
            val animation = AnimationTimeline.parse(timeline["animation"] as Map<*, *>)

            animationBlocks.addAll(animation.blocks)

        }

        // walk tree and reassign ids

        val layer = Layer(
                id = UUID.randomUUID().toString(),
                name = "vector",
                width = width,
                height = height,
                children = groups
        )

        val animationTimeline = AnimationTimeline(
                id = "45",      // Dummy id --- probably need to fix this later
                duration = duration,
                blocks = animationBlocks as List<TimelineBlock>
        )
        animationTimeline.fixDuration()

        val jsonObject = mapOf(
                "version" to 1,
                "generatedby" to "GH:/jwill/android-avd-tools",
                "layers" to layer,
                timelineToJSON(animationTimeline)
        )





        val customJsonOutput = JsonGenerator.Options().excludeNulls().build()

        val o = customJsonOutput.toJson(jsonObject)
        outputFile.writeText(JsonOutput.prettyPrint(o))

    }

    fun nextId() : Int {
        return startId++
    }

    constructor(args: Array<String>) : this() {
        val parser = DefaultParser()
        val options = Options()
        with(options) {
            addOption("h", "help", false, "Print this help message")
            addOption("time", "timeInterval", true, "Sets the time interval between keyframes for animations [in 1/100th of seconds]\nDefault: 20")
            addOption("frame", "frameInterval", true, "Sets the number of frames to skip between keyframes when exporting.\n Default: 5")
            addOption("f", "file", true, "File to convert")
        }

        val cmd = parser.parse(options, args)
        val formatter = HelpFormatter()

        if (cmd.hasOption("time")) {
            val time = cmd.getOptionValue("time").toInt()
            if (time > 0) timeInterval = time
        }
        if (cmd.hasOption("frame")) {
            val frame = cmd.getOptionValue("frame").toInt()
            if (frame > 0) frameInterval = frame
        }

        if (!cmd.hasOption("file")) {
            println("You need to specify a file.")
            formatter.printHelp("SVG2ShapeShifter", options)
        } else {
            //renderFile(cmd.getOptionValue("file"))
        }
    }

    // Dupe from ConvertSVG
    fun timelineToJSON(animation: AnimationTimeline): Pair<Any, Any> {
        return Pair(
                "timeline", mapOf(
                "animation" to animation
        )
        )
    }
}

