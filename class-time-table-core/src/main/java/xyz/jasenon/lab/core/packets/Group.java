/**
 * 
 */
package xyz.jasenon.lab.core.packets;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 版本: [1.0]
 * 功能说明: 用户群组
 * 作者: WChao 创建时间: 2017年9月21日 下午1:54:04
 */
public class Group extends Message{
	
	private static final long serialVersionUID = -3817755433171220952L;
	/**
	 * 群组ID
	 */
	private String groupId;
	/**
	 * 群组名称
	 */
	private String name;
	/**
	 * 群组头像
	 */
	private String avatar;
	/**
	 * 在线人数
	 */
	private Integer online;
	/**
	 * 组用户
	 */
	private List<ClassTimeTable> classTimeTables;

	private Group(){}

	private Group(String groupId , String name, String avatar, Integer online, List<ClassTimeTable> classTimeTables, JSONObject extras){
		this.groupId = groupId;
		this.name = name;
		this.avatar = avatar;
		this.online = online;
		this.classTimeTables = classTimeTables;
		this.extras = extras;
	}

	public static Builder newBuilder(){
		return new Builder();
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public Integer getOnline() {
		return online;
	}

	public void setOnline(Integer online) {
		this.online = online;
	}

	public List<ClassTimeTable> getUsers() {
		return classTimeTables;
	}

	public void setUsers(List<ClassTimeTable> classTimeTables) {
		this.classTimeTables = classTimeTables;
	}

	public static class Builder extends Message.Builder<Group, Builder>{
		/**
		 * 群组ID
		 */
		private String groupId;
		/**
		 * 群组名称
		 */
		private String name;
		/**
		 * 群组头像
		 */
		private String avatar;
		/**
		 * 在线人数
		 */
		private Integer online;
		/**
		 * 组用户
		 */
		private List<ClassTimeTable> classTimeTables = null;

		public Builder(){};

		public Builder groupId(String groupId) {
			this.groupId = groupId;
			return this;
		}
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		public Builder avatar(String avatar) {
			this.avatar = avatar;
			return this;
		}
		public Builder setChatType(Integer online) {
			this.online = online;
			return this;
		}
		public Builder addUser(ClassTimeTable classTimeTable) {
			if(CollectionUtils.isEmpty(classTimeTables)){
				classTimeTables = Lists.newArrayList();
			}
			classTimeTables.add(classTimeTable);
			return this;
		}
		@Override
		protected Builder getThis() {
			return this;
		}

		@Override
		public Group build(){
			return new Group(this.groupId , this.name , this.avatar , this.online , this.classTimeTables, this.extras);
		}
	}

	public Group clone(){
		Group group = Group.newBuilder().build();
		BeanUtil.copyProperties(this, group,"classTimeTables");
		return group;
	}

}
