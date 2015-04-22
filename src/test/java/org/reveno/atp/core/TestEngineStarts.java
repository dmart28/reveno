/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.core;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.commands.Result;

import com.google.common.io.Files;

public class TestEngineStarts {
	
	@Test
	@SuppressWarnings("unchecked")
	public void test() throws InterruptedException, ExecutionException {
		File baseDir = Files.createTempDir();
		Reveno engine = createEngine(baseDir);
		
		engine.startup();
		
		Assert.assertFalse(engine.query().find(LastCalculatedView.class, 1).isPresent());
		Result<Double> r = (Result<Double>) engine.executeCommand(new SqrtCommand(16)).get();
		Assert.assertTrue(r.isSuccess());
		Assert.assertEquals(r.getResult(), 4, 0.001);
		Assert.assertTrue(engine.query().find(LastCalculatedView.class, 1).isPresent());
		Assert.assertEquals(4, engine.query().find(LastCalculatedView.class, 1).get().sqrt, 0.001);
		
		engine.shutdown();
		
		System.out.println("Restart ...");
		engine = createEngine(baseDir);
		
		engine.startup();
		
		Assert.assertTrue(engine.query().find(LastCalculatedView.class, 1).isPresent());
		Assert.assertEquals(4, engine.query().find(LastCalculatedView.class, 1).get().sqrt, 0.001);
		engine.executeCommand(new SqrtCommand(64)).get();
		Assert.assertEquals(8, engine.query().find(LastCalculatedView.class, 1).get().sqrt, 0.001);
		
		engine.shutdown();
		
		baseDir.delete();
	}

	protected Reveno createEngine(File baseDir) {
		Reveno engine = new Engine(baseDir);
		engine.domain().command(SqrtCommand.class, Double.class, (c, u) -> {
			double result = Math.sqrt(c.number);
			u.executeTransaction(new WriteLastCalculationTransaction(result));
			
			return result;
		});
		engine.domain().transactionAction(WriteLastCalculationTransaction.class, (t, u) -> {
			u.repository().store(1, t.sqrt);
		});
		engine.domain().viewMapper(Double.class, LastCalculatedView.class, (e, old, r) -> {
			if (old.isPresent()) {
				old.get().sqrt = e;
				return old.get();
			} else return new LastCalculatedView(e);
		});
		return engine;
	}
	
	public static class SqrtCommand {
		public int number;
		
		public SqrtCommand(int number) {
			this.number = number;
		}
	}
	
	public static class WriteLastCalculationTransaction {
		public double sqrt;
		
		public WriteLastCalculationTransaction(double sqrt) {
			this.sqrt = sqrt;
		}
	}
	
	public static class LastCalculatedView {
		public double sqrt;
		
		public LastCalculatedView(double sqrt) {
			this.sqrt = sqrt;
		}
	}
	
}
