package net.kaikk.mc.synx.packets;

import java.util.Arrays;

public final class Node {
	private final int id;
	private final String name;
	private final String[] tags;
	
	public Node(int id, String name, String[] tags) {
		this.id = id;
		this.name = name;
		this.tags = tags;
	}
	
	public int getId() {
		return id;
	}

	public String getName() {
		return this.name;
	}
	
	public String[] getTags() {
		return this.tags;
	}

	@Override
	public String toString() {
		return "Node [id=" + id + ", name=" + name + ", tags=["+String.join(", ", this.getTags())+"]]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(tags);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(tags, other.tags))
			return false;
		return true;
	}
	
	
}
