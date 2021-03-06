package com.rnett.ligraph.eve.dotlanmaps.kframe

import com.rnett.kframe.dom.*
import com.rnett.kframe.dom.classes.*
import com.rnett.kframe.element.Style
import com.rnett.ligraph.eve.dotlanmaps.*
import kotlinx.coroutines.experimental.NonCancellable.children

fun AnyDisplayElement.dotlanMap(displayMap: DisplayRegion, width: Int = displayMap.width, height: Int = displayMap.height) = DotlanMapElement(displayMap, width, height, this)

fun AnyDisplayElement.dotlanMap(map: DotlanRegion, width: Int, height: Int): DotlanMapElement{
    val d = map.display
    return DotlanMapElement(d, width, height, this)
}
fun AnyDisplayElement.dotlanMap(map: DotlanRegion, scale: Double = 1.0) = dotlanMap(map.display(scale))

fun AnyDisplayElement.dotlanMap(regionName: String, width: Int, height: Int) = this.dotlanMap(DotlanMaps[regionName], width, height)
fun AnyDisplayElement.dotlanMap(regionID: Int, width: Int, height: Int) = this.dotlanMap(DotlanMaps[regionID], width, height)

fun AnyDisplayElement.dotlanMap(regionName: String, scale: Double = 1.0) = this.dotlanMap(DotlanMaps[regionName], scale)
fun AnyDisplayElement.dotlanMap(regionID: Int, scale: Double = 1.0) = this.dotlanMap(DotlanMaps[regionID], scale)


//could probably have just used base and relied on svg for scaling
class DotlanMapElement(val displayMap: DisplayRegion, width: Int, height: Int, parent: AnyDisplayElement) : Svg(parent, {}, "","id" to "map-${displayMap.base.regionID}") {

    constructor(displayMap: DisplayRegion, parent: AnyDisplayElement) : this(displayMap, displayMap.width, displayMap.height, parent)

    val jumps: List<DotlanJumpElement>
    val systems: Map<Int, DotlanSystemElement>

    val map = displayMap.base

    val defs: Defs get() = children.find { it.tag == "defs" } as Defs? ?: defs("")

    private val auraG: SvgElement
    private val jumpsG: SvgElement
    private val regionSysG: SvgElement
    private val externalSysG: SvgElement

    fun moveMapToFront(){
        children.removeAll(listOf(jumpsG, regionSysG, externalSysG))
        children.addAll(listOf(jumpsG, regionSysG, externalSysG))
    }

    private val gradients = mutableMapOf<Pair<String, Int>, GradientElement>()

    fun gradient(color: String, intensity: Int): String{
        gradients.getOrPut(Pair(color, intensity)){
            defs.radialGradient("grad-$color-$intensity"){
                stop("$intensity%", color)
                stop("100%", "rgb(225, 225, 225)", 0.0)
            }
        }
        return "grad-$color-$intensity"
    }

    fun setAura(systemID: Int, color: String, intensity: Int){
        val system = systems[systemID] ?: return

        removeAura(systemID)

        auraG.rect{
            fill="url(#${this@DotlanMapElement.gradient(color, intensity)})"
            width = system.systemRect.width.toDouble() + 50
            height = system.systemRect.height.toDouble() + 50
            x = system.displaySystem.xPos - 25 + system.systemRect.x.toDouble()
            y = system.displaySystem.yPos - 25 + system.systemRect.y.toDouble()
        }
    }

    fun setAllAuras(color: String, intensity: Int){
        systems.keys.forEach { setAura(it, color, intensity) }
    }

    fun setAllAuras(aura: DotlanSystemElement.() -> Pair<String, Int>){
        systems.forEach {
            val aura = aura(it.value)
            setAura(it.key, aura.first, aura.second)
        }
    }

    fun removeAura(systemID: Int){
        auraG.apply { this.children.removeIf { it.id == "aura-$systemID" } }
    }

    init{

        val mutSystems = mutableMapOf<Int, DotlanSystemElement>()
        val mutJumps = mutableListOf<DotlanJumpElement>()

        displayMap.width = width
        displayMap.height = height

        this.width = width.toDouble()
        this.height = height.toDouble()

        defs {
            g("regionSystems") {
                this@DotlanMapElement.displayMap.systems.values.filter { it.base.inRegion }.forEach {
                    mutSystems[it.base.systemID] = DotlanSystemElement(it, this@DotlanMapElement, this@g)
                }
            }
            g("externalSystems") {
                this@DotlanMapElement.displayMap.systems.values.filter { !it.base.inRegion }.forEach {
                    mutSystems[it.base.systemID] = DotlanSystemElement(it, this@DotlanMapElement, this@g)
                }
            }
        }

        jumps = mutJumps.toList()
        systems = mutSystems.toMap()

        auraG = g("auras"){

        }

        jumpsG = g("jumps") {
            this@DotlanMapElement.displayMap.jumps.forEach {
                mutJumps.add(DotlanJumpElement(it, this@DotlanMapElement, this@g))
            }
        }

        regionSysG = g("regionSysUse"){
            this@DotlanMapElement.systems.values.filter { it.system.inRegion }.forEach {
                use(it, "sysUse-${it.system.systemName}"){
                    x = it.displaySystem.xPos
                    y = it.displaySystem.yPos
                }
            }
        }
        externalSysG = g("externalSysUse"){
            this@DotlanMapElement.systems.values.filter { !it.system.inRegion }.forEach {
                use(it, "sysUse-${it.system.systemName}"){
                    x = it.displaySystem.xPos
                    y = it.displaySystem.yPos
                }
            }
        }
    }
}

class DotlanJumpElement(val displayJump: DisplayJump, val regionMap: DotlanMapElement, parent: SvgBase<*>?) : SvgElement(parent, {}, "line", "", "id" to "jump-${displayJump.base.startSystemID}-${displayJump.base.endSystemID}"){
    val jump = displayJump.base

    init{
        x1 = displayJump.startX
        y1 = displayJump.startY
        x2 = displayJump.endX
        y2 = displayJump.endY

        stroke = when(jump.type){
            DotlanJump.Type.System -> "black"
            DotlanJump.Type.Constellation -> "red"
            DotlanJump.Type.Region -> "purple"
        }

        strokeWidth = when(jump.type){
            DotlanJump.Type.System -> 1.0
            DotlanJump.Type.Constellation -> 2.0
            DotlanJump.Type.Region -> 3.0
        }

    }
}

class DotlanSystemElement(val displaySystem: DisplaySystem, val regionMap: DotlanMapElement, parent: SvgBase<*>?) : SvgElement(parent, {}, "symbol", "", "id" to "system-${displaySystem.base.systemID}"){
    val system = displaySystem.base

    val systemRect: SvgElement

    fun setAura(color: String, intensity: Int) = regionMap.setAura(system.systemID, color, intensity)

    init{
        systemRect = if(system.inRegion) {
            rect("system") {
                x = 4 * this@DotlanSystemElement.displaySystem.scaleX
                y = 3.5 * this@DotlanSystemElement.displaySystem.scaleY
                rx = 11 * this@DotlanSystemElement.displaySystem.scaleX
                ry = 11 * this@DotlanSystemElement.displaySystem.scaleY
                width = 50 * this@DotlanSystemElement.displaySystem.scaleX
                height = 22 * this@DotlanSystemElement.displaySystem.scaleY
                fill= Style.hex("e6e6e6")
                strokeWidth = 1.0
                stroke = "black"
            }
        } else {
            rect("system") {
                x = 3.5 * this@DotlanSystemElement.displaySystem.scaleX
                y = 3.5 * this@DotlanSystemElement.displaySystem.scaleY
                width = 50 * this@DotlanSystemElement.displaySystem.scaleX
                height = 22 * this@DotlanSystemElement.displaySystem.scaleY
                fill=Style.hex("b3b3b3")
                strokeWidth = 1.0
                stroke = "black"
            }
        }

        if(system.inRegion) {
            svgText("systemName") {
                +this@DotlanSystemElement.system.systemName
                x = 28 * this@DotlanSystemElement.displaySystem.scaleX
                y = 17 * this@DotlanSystemElement.displaySystem.scaleY
                textAnchor = "middle"
                style["font-size"] = (9 * this@DotlanSystemElement.displaySystem.scaleX).toInt().toString() + "px"
                style["font-family"] = "Arial, Helvetica, sans-serif"
            }
        } else {
            svgText("systemName") {
                +this@DotlanSystemElement.system.systemName
                x = 28 * this@DotlanSystemElement.displaySystem.scaleX
                y = 14 * this@DotlanSystemElement.displaySystem.scaleY
                textAnchor = "middle"
                style["font-size"] = (9 * this@DotlanSystemElement.displaySystem.scaleX).toInt().toString() + "px"
                style["font-family"] = "Arial, Helvetica, sans-serif"
            }
            svgText("regionName") {
                +this@DotlanSystemElement.system.regionName
                x = 28 * this@DotlanSystemElement.displaySystem.scaleX
                y = 21.7 * this@DotlanSystemElement.displaySystem.scaleY
                textAnchor = "middle"
                style["font-size"] = (9 * this@DotlanSystemElement.displaySystem.scaleX).toInt().toString() + "px"
                style["font-family"] = "Arial, Helvetica, sans-serif"
            }
        }
    }
}