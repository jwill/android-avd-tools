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
    MergeShapeShifter().merge(arrayOf("tree-base.shapeshifter", "tree-lights.shapeshifter"), "tree-all.shapeshifter")
    //MergeShapeShifter().merge(args)

}

class MergeShapeShifter() {
    var frameInterval = 5
    var timeInterval = 50

    var startId = 200

    fun findReferencedPaths(oldId:String, newId:String, list:ArrayList<TimelineBlock>) {
        //println()
        println("Changing refs from oldId ${oldId} to ${newId}")
        val foundIds = list.filter { it.layerId == oldId }
        foundIds.forEach { it.layerId = newId }
    }

    fun walkIds(child:PathPrimitive, timelineBlocks: ArrayList<TimelineBlock>) {
        val oldId = child.id
        child.id = nextId().toString()
        when (child) {
            is Group -> {
                findReferencedPaths(oldId, child.id, timelineBlocks)
                if (child.children.isNotEmpty()) {
                    child.children.forEach { walkIds(it, timelineBlocks) }
                }
            }
            is Path -> {
                println(child.id)
                findReferencedPaths(oldId, child.id, timelineBlocks)
            }
        }

    }

    fun merge(filenames: Array<String>, outFile:String) {
        val outputFile = File(outFile)

        val groups = arrayListOf<Group>()
        val animationBlocks = arrayListOf<TimelineBlock>()
        // Create skeleton file
        var width: Int = -1
        var height: Int = -1
        var duration: Int = -1

        val pathIdMap = mapOf<String, String>()

        for (i in filenames) {
            val file = File(i)
            val f = JsonSlurper().parse(file)
            val json: Map<Any, Any> = f as Map<Any, Any>
            val layers = json["layers"] as Map<Any, Any>
            // parse vector layer
            val vectorLayer = Layer.parse(layers["vectorLayer"] as Map<Any, Any>)
            vectorLayer.id = nextId().toString()

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

            // for each group
            // check timeline events for that original id
            // update group id and layerId in each timeline event
            // if group has children do the same
            for (child: PathPrimitive in vectorLayer.children) {
                //walkIds(child,animation.blocks as ArrayList<TimelineBlock>)
            }
        }

        // walk tree and reassign ids
       // for (i in animationBlocks) {
       //     i.id = nextId().toString()
       // }

        val layer = Layer(
                id = nextId().toString(),
                name = "vector",
                width = width,
                height = height,
                children = groups
        )

        val animationTimeline = AnimationTimeline(
                id = nextId().toString(),      // Dummy id --- probably need to fix this later
                duration = duration,
                blocks = animationBlocks as List<TimelineBlock>
        )
        animationTimeline.fixDuration()

        val jsonObject = mapOf(
                "version" to 1,
                "generatedby" to "GH:/jwill/android-avd-tools",
                "layers" to layersToJSON(layer),
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

    fun layersToJSON(layer: Layer): Map<Any, Any> {
        return mapOf(
                "vectorLayer" to layer
        )
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

