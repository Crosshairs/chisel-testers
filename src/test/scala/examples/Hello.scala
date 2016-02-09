// See LICENSE for license details.

package examples

import Chisel._
import Chisel.testers.TesterDriver
import chisel_testers._

class Hello extends Module {
  val io = new Bundle {
    val out = UInt(OUTPUT, 8)
  }
  io.out := UInt(42)
}

class HelloTester extends SteppedHWIOTester {
  val device_under_test = Module( new Hello )

  step(1)
  expect(device_under_test.io.out, 42)
}

object Hello {
  def main(args: Array[String]): Unit = {
    TesterDriver.execute { () => new HelloTester }
  }
}

class HelloSpec extends ChiselFlatSpec {
  "a" should "b" in {
    assertTesterPasses {  new HelloTester }
  }
}
