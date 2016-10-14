// See LICENSE for license details.

package chisel3.iotesters

import java.io.File

import chisel3.{HasChiselExecutionOptions, Module}
import firrtl.{ExecutionOptionsManager, HasFirrtlOptions}
import firrtl_interpreter.HasInterpreterOptions

import scala.util.DynamicVariable

object Driver {
  private val backendVar = new DynamicVariable[Option[Backend]](None)
  private[iotesters] def backend = backendVar.value

  def execute[T <: Module](
                            dutGenerator: () => T,
                            optionsManager: TesterOptionsManager
                          )
                          (
                            testerGen: T => PeekPokeTester[T]
                          ): Boolean = {
    val testerOptions = optionsManager.testerOptions

    val (dut, backend) = testerOptions.backendName match {
      case "firrtl"    =>
        setupFirrtlTerpBackend(dutGenerator, optionsManager)
      case "verilator" =>
        setupVerilatorBackend(dutGenerator)
      case "vcs"       =>
        setupVCSBackend(dutGenerator)
      case _ =>
        throw new Exception(s"Unrecognized backend name ${testerOptions.backendName}")
    }

    if(optionsManager.topName.isEmpty) {
      optionsManager.setTargetDirName(s"${optionsManager.targetDirName}/${testerGen.getClass.getName}")
    }

    backendVar.withValue(Some(backend)) {
      try {
        testerGen(dut).finish
      } catch { case e: Throwable =>
        e.printStackTrace()
        TesterProcess.killall
        throw e
      }
    }
  }

  def execute[T <: Module](args: Array[String], dut: () => T)(
    testerGen: T => PeekPokeTester[T]
  ): Boolean = {
    val optionsManager = new TesterOptionsManager

    optionsManager.parse(args) match {
      case true =>
        execute(dut, optionsManager)(testerGen)
        true
      case _ =>
        false
    }
  }
  /**
    * This is just here as command line way to see what the options are
    * It will not successfully run
    * TODO: Look into dynamic class loading as way to make this main useful
 *
    * @param args unused args
    */
  def main(args: Array[String]) {
    execute(Array("--help"), null)(null)
  }
  /**
    * Runs the ClassicTester and returns a Boolean indicating test success or failure
    * @@backendType determines whether the ClassicTester uses verilator or the firrtl interpreter to simulate the circuit
    * Will do intermediate compliation steps to setup the backend specified, including cpp compilation for the verilator backend and firrtl IR compilation for the firrlt backend
    */
  def apply[T <: Module](dutGen: () => T, backendType: String = "firrtl")(
      testerGen: T => PeekPokeTester[T]): Boolean = {
    val optionsManager = new TesterOptionsManager

    val (dut, backend) = backendType match {
      case "firrtl" => setupFirrtlTerpBackend(dutGen, optionsManager)
      case "verilator" => setupVerilatorBackend(dutGen)
      case "vcs" => setupVCSBackend(dutGen)
      case _ => throw new Exception("Unrecongnized backend type $backendType")
    }
    backendVar.withValue(Some(backend)) {
      try {
        testerGen(dut).finish
      } catch { case e: Throwable =>
        e.printStackTrace()
        TesterProcess.killall
        throw e
      }
    }
  }

  /**
    * Runs the ClassicTester using the verilator backend without doing Verilator compilation and returns a Boolean indicating success or failure
    * Requires the caller to supply path the already compile Verilator binary
    */
  def run[T <: Module](dutGen: () => T, cmd: Seq[String])
                      (testerGen: T => PeekPokeTester[T]): Boolean = {
    val circuit = chisel3.Driver.elaborate(dutGen)
    val dut = getTopModule(circuit).asInstanceOf[T]
    backendVar.withValue(Some(new VerilatorBackend(dut, cmd))) {
      try {
        testerGen(dut).finish
      } catch { case e: Throwable =>
        e.printStackTrace()
        TesterProcess.killall
        throw e
      }
    }
  }

  def run[T <: Module](dutGen: () => T, binary: String, args: String*)
                      (testerGen: T => PeekPokeTester[T]): Boolean =
    run(dutGen, binary +: args.toSeq)(testerGen)

  def run[T <: Module](dutGen: () => T, binary: File, waveform: Option[File] = None)
                      (testerGen: T => PeekPokeTester[T]): Boolean = {
    val args = waveform match {
      case None => Nil
      case Some(f) => Seq(s"+waveform=$f")
    }
    run(dutGen, binary.toString +: args.toSeq)(testerGen)
  }
}
