// See LICENSE for license details.

package examples

import chisel3._
import chisel3.iotesters.{SteppedHWIOTester, ChiselFlatSpec, Exerciser}

class Adder(val w: Int) extends Module {
  val io = new Bundle {
    val in0 = UInt(INPUT,  w)
    val in1 = UInt(INPUT,  w)
    val out = UInt(OUTPUT, w)
  }
// printf("in0 %d in1 %d result %d\n", io.in0, io.in1, io.out)
  io.out := io.in0 + io.in1
}

class AdderTests extends SteppedHWIOTester {
  val device_under_test = Module( new Adder(10) )
  val c = device_under_test
  enable_all_debug = true

  rnd.setSeed(0L)
  for (i <- 0 until 10) {
    val in0 = rnd.nextInt(1 << c.w)
    val in1 = rnd.nextInt(1 << c.w)
    poke(c.io.in0, in0)
    poke(c.io.in1, in1)
    expect(c.io.out, (in0 + in1) & ((1 << c.w) - 1))

    step(1)
  }
}

class AdderExerciser extends Exerciser {
  val width = 32
  val (x_range_start, y_range_start) = (3, 7)
  val device_under_test = Module( new Adder(internal_counter_width) )
  val c = device_under_test

  printf(s"state_number %d, ticker %d, state_locked %x max_ticks %d",
    state_number, ticker, state_locked, max_ticks_for_state)

  def range(start:Int): Range = {
    val count = 20 // this forces ranges to all be the same size
    Range(start, start + count)
  }
  val in0_vec = Vec(range(x_range_start).map(UInt(_)))
  val in1_vec = Vec(range(y_range_start).map(UInt(_)))

  val expected_out_vec = Vec(in0_vec.zip(in1_vec).map { case (i,j) => i + j })
  val test_number      = Reg(init=UInt(0, width = internal_counter_width))

  buildState("check adder")(StopCondition(test_number > UInt(range(0).size))) { () =>
    printf(
      "%d ticker %d test# %d : %d + %d => %d expected %d\n",
      state_number, ticker, test_number,
      in0_vec(test_number), in1_vec(test_number),
      in0_vec(test_number) + in1_vec(test_number),
      expected_out_vec(test_number)
    )
    assert(expected_out_vec(test_number) === in0_vec(test_number) + in1_vec(test_number))
    test_number := test_number + UInt(1)
  }
  finish()
}

class AdderGo extends ChiselFlatSpec {
  "adder" should "add things properly" in {
    assertTesterPasses { new AdderExerciser }
  }
}

class AdderTester extends ChiselFlatSpec {
  "Adder" should "compile and run without incident" in {
    assertTesterPasses { new AdderTests }
  }
}
