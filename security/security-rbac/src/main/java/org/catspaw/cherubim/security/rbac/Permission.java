package org.catspaw.cherubim.security.rbac;

/**
 * ��Ȩ����Ȩ=��Դ+����
 * @author sunnz
 */
public interface Permission {

	String getCode();

	String getDomainCode();

	String getActionCode();
	
	String getInstance();
}
