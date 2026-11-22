package com.epam.rd.autocode.assessment.appliances.persistence;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConstraintFailureEdgesTest {
 @Test void onlyTheExpectedOperationAndNamedConstraintAreTranslated(){
  var extractor=mock(DatabaseConstraintExtractor.class);var mapper=new PersistenceConflictTranslator(extractor);var failure=new DataIntegrityViolationException("private");
  Object[][] cases={{PersistenceOperation.CLIENT_DELETE,"fk_orders_client","client.in-use"},{PersistenceOperation.EMPLOYEE_DELETE,"fk_orders_employee","employee.in-use"},{PersistenceOperation.MANUFACTURER_DELETE,"fk_appliance_manufacturer","manufacturer.in-use"},{PersistenceOperation.APPLIANCE_DELETE,"fk_order_row_appliance","appliance.in-use"},{PersistenceOperation.APPLIANCE_WRITE,"fk_appliance_manufacturer","appliance.manufacturer.not-found"},{PersistenceOperation.ORDER_WRITE,"fk_orders_client","order.client.not-found"},{PersistenceOperation.ORDER_WRITE,"fk_order_row_appliance","order.appliance.not-found"},{PersistenceOperation.ORDER_WRITE,"fk_orders_employee","order.employee.unavailable"}};
  for(Object[] c:cases){when(extractor.extract(failure)).thenReturn(List.of(new ConstraintFailure("23503",(String)c[1])));assertThat(mapper.translate((PersistenceOperation)c[0],failure)).isNotSameAs(failure).hasCause(failure);when(extractor.extract(failure)).thenReturn(List.of(new ConstraintFailure("23506","unknown")));assertThat(mapper.translate((PersistenceOperation)c[0],failure)).isSameAs(failure);}
  for(String state:new String[]{"23505","unknown"}){when(extractor.extract(failure)).thenReturn(List.of(new ConstraintFailure(state,"unknown")));assertThat(mapper.translate(PersistenceOperation.ACCOUNT_WRITE,failure)).isSameAs(failure);}
  when(extractor.extract(failure)).thenReturn(List.of(new ConstraintFailure("23503","fk_orders_client")));assertThat(mapper.translate(PersistenceOperation.ACCOUNT_WRITE,failure)).isSameAs(failure);
 }
 @Test void cyclicAndLongExceptionGraphsAreBoundedAndSqlChainsRemainInspectable(){
  var extractor=new DatabaseConstraintExtractor();var a=new RuntimeException();var b=new RuntimeException(a);a.initCause(b);assertThat(extractor.extract(a)).isEmpty();
  RuntimeException top=new RuntimeException();for(int i=0;i<40;i++)top=new RuntimeException(top);assertThat(extractor.extract(top)).isEmpty();
  var sql=new SQLException("private","23505");sql.setNextException(new SQLException("private","23503"));assertThat(extractor.extract(new RuntimeException(sql))).isEmpty();
  var h2=mock(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);when(h2.getMessage()).thenReturn(null);assertThat(extractor.extract(new RuntimeException(h2))).isEmpty();
  when(h2.getMessage()).thenReturn("unrecognized private header");assertThat(extractor.extract(new RuntimeException(h2))).isEmpty();
 }
}
