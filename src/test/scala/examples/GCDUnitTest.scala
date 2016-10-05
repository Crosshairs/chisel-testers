// See LICENSE for license details.

package examples

import chisel3._
import chisel3.util._
import chisel3.iotesters.{SteppedHWIOTester, ChiselFlatSpec}
import chisel3.Strict.CompileOptions

class GCD extends Module {
  val int_width = 16
  val io = new Bundle {
    val a  = UInt(INPUT,  width = int_width)
    val b  = UInt(INPUT,  width = int_width)
    val e  = Bool(INPUT)
    val z  = UInt(OUTPUT, width = int_width)
    val v  = Bool(OUTPUT)
  }
  val x  = Reg(UInt(width = int_width))
  val y  = Reg(UInt(width = int_width))
  when   (x > y) { x := x - y }
  unless (x > y) { y := y - x }
  when (io.e) { x := io.a; y := io.b }
  io.z := x
  io.v := y === UInt(0)
}

class GCDUnitTester extends SteppedHWIOTester {
  def computeGcd(a: Int, b: Int): (Int, Int) = {
    var x = a
    var y = b
    var depth = 1
    while(y > 0 ) {
      if (x > y) {
        x -= y
      }
      else {
        y -= x
      }
      depth += 1
    }
    (x, depth)
  }

  val (a, b, z) = (64, 48, 16)
  val device_under_test = Module(new GCD)
  val gcd = device_under_test

  poke(gcd.io.a, a)
  poke(gcd.io.b, b)
  poke(gcd.io.e, 1)
  step(1)
  poke(gcd.io.e, 0)

  val (expected_gcd, steps) = computeGcd(a, b)

  step(steps - 1) // -1 is because we step(1) already to toggle the enable
  expect(gcd.io.z, expected_gcd)
  expect(gcd.io.v, 1)
}

class GCDTester extends ChiselFlatSpec {
  "a" should "b" in {
    assertTesterPasses { new GCDUnitTester }
  }
}

//object GCDUnitTest {
//  def main(args: Array[String]): Unit = {
//    val tutorial_args = args.slice(1, args.length)
//
//    new GCDTester
//  }
//}
//
