package org.reveno.atp.core.serialization;

import java.io.Serializable;

public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	public String getName() {
		return name;
	}
	
	private int age;
	public int getAge() {
		return age;
	}
	
	public User(String name, int age) {
		this.name = name;
		this.age = age;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof User))
			return false;
		return ((User)obj).name.equals(name) && ((User)obj).age == age;
	}
}