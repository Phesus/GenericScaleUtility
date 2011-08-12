/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genericscaleutility

import groovy.swing.SwingBuilder

/**
 *
 * @author SYSTEM
 */
class Control {
    static def comMan = new ComMan()
    static def init() {
            SwingBuilder.build {
                lookAndFeel( 'nimbus' )
            }
            def wnd = new MainWindow()
            wnd.setVisible true
            wnd.btnGet.actionPerformed = {
                def command        = Eval.me(wnd.txtCommand.text)
                def mD             = [port:wnd.txtPort.text, baud:wnd.txtBaud.text as int, bits:wnd.bits.selectedItem,
                                      stopBits:wnd.stopBits.selectedItem, parity:wnd.parity.selectedItem,
                                      stopChar:wnd.stopChar.text]
                wnd.txtOutput.text = getWeight(command, mD)
            }
        }
    static def getWeight(command, miniDriver) {
            println "Pidiendo peso con comando: $command"
            //comMan.write(command.getBytes())
            comMan.readWeight(command.getBytes(), miniDriver)
        }
}

