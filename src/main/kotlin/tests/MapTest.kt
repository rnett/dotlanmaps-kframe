package tests

import com.rnett.kframe.dom.body
import com.rnett.kframe.hosts.web.site
import com.rnett.ligraph.eve.dotlanmaps.kframe.dotlanMap
import io.ktor.application.Application


fun Application.mapTest() {
    site {

        page("") {
            body {
                val map = dotlanMap("The Spire", 1700, 930)

                map.setAllAuras {
                    if(system.inRegion){
                        Pair("orange", 40)
                    } else
                        Pair("purple", 20)
                }

                map.systems.values.find { it.system.systemName == "C-BHDN" }?.setAura("blue", 60)
            }
        }
    }
}