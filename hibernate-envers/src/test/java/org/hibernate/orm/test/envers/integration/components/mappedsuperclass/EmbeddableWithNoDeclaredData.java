/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.components.mappedsuperclass;

import jakarta.persistence.Embeddable;

/**
 * @author Jakob Braeuchi.
 * @author Gail Badner
 */
@Embeddable
public class EmbeddableWithNoDeclaredData extends AbstractEmbeddable {

	public EmbeddableWithNoDeclaredData(int code) {
		super( code );
	}

	// Needed for @Embeddable
	protected EmbeddableWithNoDeclaredData() {
	}
}
