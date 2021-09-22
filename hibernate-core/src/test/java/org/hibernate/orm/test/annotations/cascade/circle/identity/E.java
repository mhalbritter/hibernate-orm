/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade.circle.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class E extends AbstractEntity {
    private static final long serialVersionUID = 1226955558L;

    /**
     * No documentation
     */
    @jakarta.persistence.ManyToOne(cascade =  {
        jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
    , optional = false)
    private F f;

    public F getF() {
        return f;
    }

    public void setF(F parameter) {
        this.f = parameter;
    }
}
