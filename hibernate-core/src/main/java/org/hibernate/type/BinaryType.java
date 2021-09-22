/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;
import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.VarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between a {@link Types#VARBINARY VARBINARY} and {@code byte[]}
 *
 * Implementation of the {@link VersionType} interface should be considered deprecated.
 * For binary entity versions/timestamps, {@link RowVersionType} should be used instead.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BinaryType
		extends AbstractSingleColumnStandardBasicType<byte[]>
		implements VersionType<byte[]>, AdjustableBasicType<byte[]> {

	public static final BinaryType INSTANCE = new BinaryType();

	public String getName() {
		return "binary";
	}

	public BinaryType() {
		super( VarbinaryTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "byte[]", byte[].class.getName() };
	}

	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 * @deprecated use {@link RowVersionType} for binary entity versions/timestamps
	 */
	@Override
	@Deprecated
	public byte[] seed(SharedSessionContractImplementor session) {
		// Note : simply returns null for seed() and next() as the only known
		// 		application of binary types for versioning is for use with the
		// 		TIMESTAMP datatype supported by Sybase and SQL Server, which
		// 		are completely db-generated values...
		return null;
	}

	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 * @deprecated use {@link RowVersionType} for binary entity versions/timestamps
	 */
	@Override
	@Deprecated
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}

	/**
	 * Get a comparator for version values.
	 *
	 * @return The comparator to use to compare different version values.
	 * @deprecated use {@link RowVersionType} for binary entity versions/timestamps
	 */
	@Override
	@Deprecated
	public Comparator<byte[]> getComparator() {
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.getComparator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeDescriptorIndicators indicators, JavaTypeDescriptor<X> domainJtd) {
		if ( ! indicators.isLob() ) {
			return (BasicType<X>) this;
		}

		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
		final JdbcTypeDescriptor jdbcType = jdbcTypeRegistry.getDescriptor( Types.BLOB );

		return typeConfiguration.getBasicTypeRegistry().resolve( domainJtd, jdbcType, getName() );
	}
}
