/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.MonetaryAmount;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table( name = "payments" )
public abstract class Payment {
	private Integer id;
	private MonetaryAmount amount;

	public Payment() {
	}

	public Payment(Integer id, MonetaryAmount amount) {
		this.id = id;
		this.amount = amount;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public MonetaryAmount getAmount() {
		return amount;
	}

	public void setAmount(MonetaryAmount amount) {
		this.amount = amount;
	}
}
