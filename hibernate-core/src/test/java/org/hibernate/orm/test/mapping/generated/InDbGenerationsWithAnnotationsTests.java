/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.sql.Timestamp;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.tuple.GenerationTiming;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link CreationTimestamp} and {@link UpdateTimestamp}
 * annotations with Timestamp-valued attributes
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = InDbGenerationsWithAnnotationsTests.AuditedEntity.class )
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class, comment = "We rely on current_timestamp being the SQL function name")
@RequiresDialectFeature(feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class, comment = "Without this, we might not see an update to the timestamp")
public class InDbGenerationsWithAnnotationsTests {
	@Test
	public void testGenerations(SessionFactoryScope scope) {
		scope.inSession( (session) -> {
			// first creation
			final AuditedEntity saved = scope.fromTransaction( session, (s) -> {
				final AuditedEntity entity = new AuditedEntity( 1, "it" );
				session.persist( entity );
				return entity;
			} );

			assertThat( saved ).isNotNull();
			assertThat( saved.createdOn ).isNotNull();
			assertThat( saved.lastUpdatedOn ).isNotNull();

			saved.name = "changed";

			// then changing
			final AuditedEntity merged = scope.fromTransaction( session, (s) -> {
				return (AuditedEntity) session.merge( saved );
			} );

			assertThat( merged ).isNotNull();
			assertThat( merged.createdOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotEqualTo( merged.createdOn );

			// lastly, make sure we can load it..
			final AuditedEntity loaded = scope.fromTransaction( session, (s) -> {
				return session.get( AuditedEntity.class, 1 );
			} );

			assertThat( loaded ).isNotNull();
			assertThat( loaded.createdOn ).isEqualTo( merged.createdOn );
			assertThat( loaded.lastUpdatedOn ).isEqualTo( merged.lastUpdatedOn );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createQuery( "delete AuditedEntity" ).executeUpdate() );
	}

	@Entity( name = "AuditedEntity" )
	@Table( name = "ann_generated_simple" )
	public static class AuditedEntity {
		@Id
		public Integer id;
		@Basic
		public String name;

		@CurrentTimestamp( timing = GenerationTiming.INSERT )
		public Timestamp createdOn;
		@CurrentTimestamp( timing = GenerationTiming.ALWAYS )
		public Timestamp lastUpdatedOn;

		public AuditedEntity() {
		}

		public AuditedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
