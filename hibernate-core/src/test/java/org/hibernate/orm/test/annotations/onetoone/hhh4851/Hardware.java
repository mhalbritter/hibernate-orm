/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.onetoone.hhh4851;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "DeviceType", length = 1)
@DiscriminatorValue(value = "C")
public class Hardware extends BaseEntity {

	private Hardware parent = null;

	protected Hardware() {

	}

	public Hardware(Hardware parent) {
		this.parent = parent;

	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	public Hardware getParent() {
		return this.parent;
	}

	public void setParent(Hardware parent) {
		this.parent = parent;
	}

}
