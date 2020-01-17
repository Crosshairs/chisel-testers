/*
Copyright 2020 The Regents of the University of California (Regents)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package examples

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{FreeSpec, Matchers}

/**
  * This provides an example of how to re-run a verilator compiled simulation without re-running verilator
  * Currently this method does re-elaborate the circuit
  */
class RerunWithoutElaboratonAndCompileSpec extends FreeSpec with Matchers {
  "Demonstrate how to re-run a given test without recompiling" - {
    "build once" in {
      iotesters.Driver.execute(
        Array("--backend-name", "verilator", "--target-dir", "test_run_dir/build1", "--top-name", "PlusOne"), () => new PlusOne
      ) { c =>
        new PlusOneTester(c, 0)
      } should be(true)

      iotesters.Driver.run(() => new PlusOne, "./test_run_dir/build1/VPlusOne") { c =>
        new PlusOneTester(c, 33333)
      }
    }
  }
}

class PlusOneTester(c: PlusOne, start_number: Int) extends PeekPokeTester(c) {
  for(i <- start_number to start_number + 10000) {
    poke(c.io.dog, i)
    poke(c.io.cat, i)
    poke(c.io.fox, i)
    poke(c.io.asp, i)
    expect(c.io.out, i + 1)
  }
}

class PlusOne extends Module {
  val io = IO(new Bundle {
    val dog = Input(UInt(64.W))
    val cat = Input(UInt(64.W))
    val asp = Input(UInt(64.W))
    val fox = Input(UInt(64.W))
    val out = Output(UInt(64.W))
  })

  io.out := io.fox +% 1.U
}
