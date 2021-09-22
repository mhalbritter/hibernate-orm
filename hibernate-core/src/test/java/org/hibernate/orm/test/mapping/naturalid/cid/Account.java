/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.cid;

import jakarta.persistence.Basic;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;

/**
 * @author Donnchadh O Donnabhain
 */
@Entity
@Table( name = "t_acct" )
public class Account {
    @EmbeddedId
    private AccountId accountId;

    @Basic( optional = false )
    @NaturalId
    private String shortCode;
    
    protected Account() {
    }
    
    public Account(AccountId accountId, String shortCode) {
        this.accountId = accountId;
        this.shortCode = shortCode;
    }
    public String getShortCode() {
        return shortCode;
    }
    public AccountId getAccountId() {
        return accountId;
    }
}
