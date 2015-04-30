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

package org.reveno.atp.acceptance.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.handlers.Commands;
import org.reveno.atp.acceptance.handlers.Transactions;
import org.reveno.atp.acceptance.model.immutable.ImmutableAccount;
import org.reveno.atp.acceptance.model.mutable.MutableAccount;
import org.reveno.atp.api.Configuration.CpuConsumption;
import org.reveno.atp.api.Configuration.ModelType;
import org.reveno.atp.api.Reveno;
import org.reveno.atp.core.Engine;
import org.reveno.atp.test.utils.FileUtils;

import com.google.common.io.Files;

@RunWith(Parameterized.class)
public class Tests {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { ModelType.IMMUTABLE }, { ModelType.MUTABLE }  
           });
    }
    
    @Parameter
    public ModelType modelType;
	
	private File tempDir;
	
	@Before
	public void setUp() {
		tempDir = Files.createTempDir();
	}
	
	@After
	public void tearDown() throws IOException {
		FileUtils.delete(tempDir);
	}

	@Test 
	public void testBasic() throws InterruptedException, ExecutionException {
		Reveno reveno = createEngine();
		reveno.startup();
		
		reveno.executeCommand(new CreateNewAccountCommand("RUB", 1000L)).get();
		
		reveno.shutdown();
	}
	
	protected Reveno createEngine() {
		return createEngine((r) -> {});
	}
	
	protected Reveno createEngine(Consumer<Reveno> interceptor) {
		Reveno reveno = new Engine(tempDir);
		
		reveno.config().cpuConsumption(CpuConsumption.PHASED);
		reveno.config().modelType(modelType);
		
		reveno.domain().command(CreateNewAccountCommand.class, Long.class, Commands::createAccount);
		reveno.domain().transactionAction(CreateAccount.class, Transactions::createAccount);
		reveno.domain().transactionAction(Credit.class, Transactions::credit);
		reveno.domain().transactionAction(Debit.class, Transactions::debit);
		
		interceptor.accept(reveno);
		
		if (modelType == ModelType.IMMUTABLE) {
			Transactions.accountFactory = ImmutableAccount.FACTORY;
		} else {
			Transactions.accountFactory = MutableAccount.FACTORY;
		}
		
		return reveno;
	}
	
}
