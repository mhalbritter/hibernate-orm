/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.component.basic;

import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.TemporalUnit;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author Gavin King
 */
public class ComponentTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "component/basic/User.hbm.xml" };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void afterMetadataBuilt(Metadata metadata) {
		// Oracle and Postgres do not have year() functions, so we need to
		// redefine the 'User.person.yob' formula
		//
		// consider temporary until we add the capability to define
		// mapping formulas which can use dialect-registered functions...
		PersistentClass user = metadata.getEntityBinding( User.class.getName() );
		org.hibernate.mapping.Property personProperty = user.getProperty( "person" );
		Component component = ( Component ) personProperty.getValue();
		Formula f = ( Formula ) component.getProperty( "yob" ).getValue().getColumnIterator().next();

		String pattern = metadata.getDatabase().getJdbcEnvironment().getDialect().extractPattern( TemporalUnit.YEAR );
		String formula = pattern.replace( "?1", "YEAR" ).replace( "?2", "dob" );
		f.setFormula( formula );
	}

	@Test
	public void testUpdateFalse() {
		sessionFactory().getStatistics().clear();
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
		s.persist(u);
		s.flush();
		u.getPerson().setName("XXXXYYYYY");
		t.commit();
		s.close();
		
		assertEquals( 1, sessionFactory().getStatistics().getEntityInsertCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityUpdateCount() );

		s = openSession();
		t = s.beginTransaction();
		u = s.get(User.class, "gavin");
		assertEquals( u.getPerson().getName(), "Gavin King" );
		s.delete(u);
		t.commit();
		s.close();
		
		assertEquals( 1, sessionFactory().getStatistics().getEntityDeleteCount() );
	}
	
	@Test
	public void testComponent() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
		s.persist(u);
		s.flush();
		u.getPerson().changeAddress("Phipps Place");
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		u = s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPerson().getYob(), u.getPerson().getDob().getYear()+1900 );
		u.setPassword("$ecret");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u = s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPassword(), "$ecret" );
		s.delete(u);
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2366" )
	public void testComponentStateChangeAndDirtiness() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
		s.persist( u );
		s.flush();
		long intialUpdateCount = sessionFactory().getStatistics().getEntityUpdateCount();
		u.getPerson().setAddress( "Austin" );
		s.flush();
		assertEquals( intialUpdateCount + 1, sessionFactory().getStatistics().getEntityUpdateCount() );
		intialUpdateCount = sessionFactory().getStatistics().getEntityUpdateCount();
		u.getPerson().setAddress( "Cedar Park" );
		s.flush();
		assertEquals( intialUpdateCount + 1, sessionFactory().getStatistics().getEntityUpdateCount() );
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testComponentQueries() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.save( emp );

		s.createQuery( "from Employee e where e.person = :p and 1 = 1 and 2=2" ).setParameter( "p", emp.getPerson() ).list();
		s.createQuery( "from Employee e where :p = e.person" ).setParameter( "p", emp.getPerson() ).list();
		// The following fails on Sybase due to HHH-3510. When HHH-3510 
		// is fixed, the check for SybaseASE15Dialect should be removed.
		if ( ! ( getDialect() instanceof SybaseASE15Dialect ) ) {
			s.createQuery(
							"from Employee e where e.person = ('', '', current_timestamp, 0.0, 'steve', '', 0)" )
					.list();
		}

		s.delete( emp );
		t.commit();
		s.close();
	}

	@Test
	@RequiresDialect( value = SybaseASEDialect.class )
	@FailureExpected( jiraKey = "HHH-3150" )
	public void testComponentQueryMethodNoParensFailureExpected() {
		// Sybase should translate "current_timestamp" in HQL to "getdate()";
		// This fails currently due to HHH-3510. The following test should be
		// deleted and testComponentQueries() should be updated (as noted
		// in that test case) when HHH-3510 is fixed.
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.save( emp );
		s.createQuery( "from Employee e where e.person = (current_timestamp, 'steve')" ).list();
		s.delete( emp );
		t.commit();
		s.close();
	}

	@Test
	public void testComponentFormulaQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from User u where u.person.yob = 1999").list();
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
		Root<User> root = criteria.from( User.class );
		Join<Object, Object> person = root.join( "person", JoinType.INNER );
		criteria.where( criteriaBuilder.between( person.get( "yob" ), new Integer(1999), new Integer(2002) ) );
		s.createQuery( criteria ).list();

//		s.createCriteria(User.class)
//			.add( Property.forName("person.yob").between( new Integer(1999), new Integer(2002) ) )
//			.list();

		s.createQuery("from User u where u.person = ('Peachtree Rd', 'Peachtree Rd', :dob, 34, 'gavin', 'Karbarook Ave', 1974)")
			.setParameter("dob", new Date("March 25, 1974")).list();
		s.createQuery("from User where person = ('Peachtree Rd', 'Peachtree Rd', :dob, 34, 'gavin', 'Karbarook Ave', 1974)")
			.setParameter("dob", new Date("March 25, 1974")).list();
		t.commit();
		s.close();
	}
	
	@Test
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
		final double HEIGHT_INCHES = 73;
		final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
		u.getPerson().setHeightInches(HEIGHT_INCHES);
		s.persist( u );
		s.flush();
		
		// Test value conversion during insert
		// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
		// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
		Double heightViaSql =
				( (Number)s.createNativeQuery("select height_centimeters from T_USER where T_USER.userName='steve'").uniqueResult())
						.doubleValue();
		assertEquals(HEIGHT_CENTIMETERS, heightViaSql, 0.01d);

		// Test projection
		Double heightViaHql = (Double)s.createQuery("select u.person.heightInches from User u where u.id = 'steve'").uniqueResult();
		assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);
		
		// Test restriction and entity load via criteria
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
		Root<User> root = criteria.from( User.class );
		Join<Object, Object> person = root.join( "person", JoinType.INNER );
		criteria.where( criteriaBuilder.between( person.get( "heightInches" ), HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d) );
		u = s.createQuery( criteria ).uniqueResult();
//		u = (User)s.createCriteria(User.class)
//			.add(Restrictions.between("person.heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//			.uniqueResult();
		assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);
		
		// Test predicate and entity load via HQL
		u = (User)s.createQuery("from User u where u.person.heightInches between ?1 and ?2")
			.setParameter(1, HEIGHT_INCHES - 0.01d)
			.setParameter(2, HEIGHT_INCHES + 0.01d)
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);
		
		// Test update
		u.getPerson().setHeightInches(1);
		s.flush();
		heightViaSql =
				( (Number)s.createNativeQuery("select height_centimeters from T_USER where T_USER.userName='steve'").uniqueResult() )
						.doubleValue();
		assertEquals(2.54d, heightViaSql, 0.01d);
		s.delete(u);
		t.commit();
		s.close();
	}
	
	@Test
	public void testNamedQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.getNamedQuery("userNameIn")
			.setParameterList( "nameList", new Object[] {"1ovthafew", "turin", "xam"} )
			.list();
		t.commit();
		s.close();
	}

	@Test
	public void testMergeComponent() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.persist( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		t.commit();
		s.close();

		assertNull(emp.getOptionalComponent());
		emp.setOptionalComponent( new OptionalComponent() );
		emp.getOptionalComponent().setValue1( "emp-value1" );
		emp.getOptionalComponent().setValue2( "emp-value2" );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		t.commit();
		s.close();

		assertEquals("emp-value1", emp.getOptionalComponent().getValue1());
		assertEquals("emp-value2", emp.getOptionalComponent().getValue2());
		emp.getOptionalComponent().setValue1( null );
		emp.getOptionalComponent().setValue2( null );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertNull(emp.getOptionalComponent());

		Employee emp1 = new Employee();
		emp1.setHireDate( new Date() );
		emp1.setPerson( new Person() );
		emp1.getPerson().setName( "bozo" );
		emp1.getPerson().setDob( new Date() );
		emp.getDirectReports().add( emp1 );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertNull( emp1.getOptionalComponent() );
		emp1.setOptionalComponent( new OptionalComponent() );
		emp1.getOptionalComponent().setValue1( "emp1-value1" );
		emp1.getOptionalComponent().setValue2( "emp1-value2" );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertEquals( "emp1-value1", emp1.getOptionalComponent().getValue1());
		assertEquals( "emp1-value2", emp1.getOptionalComponent().getValue2());
		emp1.getOptionalComponent().setValue1( null );
		emp1.getOptionalComponent().setValue2( null );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertNull(emp1.getOptionalComponent());

		s = openSession();
		t = s.beginTransaction();
		s.delete( emp );
		t.commit();
		s.close();
	}

}

