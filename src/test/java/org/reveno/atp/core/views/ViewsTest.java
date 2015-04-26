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

package org.reveno.atp.core.views;

import static org.reveno.atp.utils.BinaryUtils.longToBytes;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;

import org.junit.Test;
import org.reveno.atp.api.domain.WriteableRepository;
import org.reveno.atp.core.engine.components.RecordingRepository;
import org.reveno.atp.core.repository.HashMapRepository;

public class ViewsTest {

	@SuppressWarnings("serial")
	@Test
	public void test() {
		ViewsManager manager = new ViewsManager();
		manager.register(User.class, UserView.class, (e, ov, r) -> new UserView(UUID.nameUUIDFromBytes(longToBytes(e.id)).toString(), 
				r.get(UserInfo.class, e.id).get().name, r.get(UserInfo.class, e.id).get().age));
		ViewsDefaultStorage storage = new ViewsDefaultStorage(new HashMapRepository());
		
		WriteableRepository repository = new HashMapRepository();
		repository.store(1, new User(1, "123"));
		repository.store(1, new UserInfo(1, "Artem", 22));
		
		Assert.assertEquals(0, storage.select(UserView.class).size());
		
		ViewsProcessor processor = new ViewsProcessor(manager, storage, repository);
		processor.process(new HashMap<Class<?>, Set<Long>>() {{
			put(User.class, new LinkedHashSet<Long>());
			get(User.class).addAll(RecordingRepository.GET_ALL);
			put(UserInfo.class, new LinkedHashSet<Long>());
			get(UserInfo.class).addAll(RecordingRepository.GET_ALL);
		}});
		
		Assert.assertEquals(1, storage.select(UserView.class).size());
		UserView uv = storage.find(UserView.class, 1).get();
		Assert.assertEquals("Artem", uv.name);
		Assert.assertEquals(22, uv.age);
		
		processor.process(new HashMap<Class<?>, Set<Long>>() {{
			put(User.class, new LinkedHashSet<Long>());
			get(User.class).add(-1L);
		}});
		Assert.assertEquals(0, storage.select(UserView.class).size());
	}
	
	public static class User {
		public long id;
		public String password;
		
		public User(long id, String password) {
			this.id = id;
			this.password = password;
		}
	}
	
	public static class UserInfo {
		public long userId;
		public String name;
		public int age;
		
		public UserInfo(long userId, String name, int age) {
			this.userId = userId;
			this.name = name;
			this.age = age;
		}
	}
	
	public static class UserView {
		public String id;
		public String name;
		public int age;
		
		public UserView(String id, String name, int age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}
	}
	
}
