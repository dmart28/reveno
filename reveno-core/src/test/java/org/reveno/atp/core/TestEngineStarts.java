package org.reveno.atp.core;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.api.commands.Result;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TestEngineStarts {
	
	@Test
	@SuppressWarnings("unchecked")
	public void test() throws InterruptedException, ExecutionException {
		File baseDir = Files.createTempDir();
		Reveno engine = createEngine(baseDir);
		
		engine.startup();
		
		Assert.assertFalse(engine.query().findO(LastCalculatedView.class, 1).isPresent());
		Result<Double> r = engine.<Double>executeCommand(new SqrtCommand(16)).get();
		Assert.assertTrue(r.isSuccess());
		Assert.assertEquals(r.getResult(), 4, 0.001);
		Assert.assertTrue(engine.query().findO(LastCalculatedView.class, 1).isPresent());
		Assert.assertEquals(4, engine.query().findO(LastCalculatedView.class, 1).get().sqrt, 0.001);

		engine.shutdown();
		
		System.out.println("Restart ...");
		engine = createEngine(baseDir);
		
		engine.startup();
		
		Assert.assertTrue(engine.query().findO(LastCalculatedView.class, 1).isPresent());
		Assert.assertEquals(4, engine.query().find(LastCalculatedView.class, 1).sqrt, 0.001);
		engine.executeCommand(new SqrtCommand(64)).get();
		// 3 because we call u.id(..) twice in command handler
		Assert.assertEquals(8, engine.query().find(LastCalculatedView.class, 3).sqrt, 0.001);
		
		engine.shutdown();
		
		baseDir.delete();
	}

	protected Reveno createEngine(File baseDir) {
		Reveno engine = new Engine(baseDir);
		engine.domain().command(SqrtCommand.class, Double.class, (c, u) -> {
			double result = Math.sqrt(c.number);
			u.executeTxAction(new WriteLastCalculationTransaction(u.id(SqrtCommand.class), result));
			
			// test next id generation as well
			u.id(SqrtCommand.class);
			
			return result;
		});
		engine.domain().transactionAction(WriteLastCalculationTransaction.class, (t, u) -> {
			u.repo().store(t.id, t.sqrt);
		});
		engine.domain().viewMapper(Double.class, LastCalculatedView.class, (id,e,r) -> {
			Optional<LastCalculatedView> old = r.getO(LastCalculatedView.class, id);
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
		public long id;
		public double sqrt;
		
		public WriteLastCalculationTransaction(long id, double sqrt) {
			this.id = id;
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
