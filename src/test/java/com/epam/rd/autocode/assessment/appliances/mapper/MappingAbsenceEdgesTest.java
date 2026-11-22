package com.epam.rd.autocode.assessment.appliances.mapper;
import com.epam.rd.autocode.assessment.appliances.model.*;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.*;
class MappingAbsenceEdgesTest {
 @Test void absentDtosDoNotReplaceExistingEntities(){
  var clients=Mappers.getMapper(ClientMapper.class);var c=new Client();c.setName("unchanged");clients.applyRequest(null,c);assertThat(c.getName()).isEqualTo("unchanged");assertThat(clients.toResponse(null)).isNull();assertThat(clients.toRequest(null)).isNull();
  var employees=Mappers.getMapper(EmployeeMapper.class);var e=new Employee();e.setName("unchanged");employees.applyRequest(null,e);assertThat(e.getName()).isEqualTo("unchanged");assertThat(employees.toResponse(null)).isNull();assertThat(employees.toRequest(null)).isNull();
  var manufacturers=Mappers.getMapper(ManufacturerMapper.class);var m=new Manufacturer();m.setName("unchanged");manufacturers.applyRequest(null,m);assertThat(m.getName()).isEqualTo("unchanged");assertThat(manufacturers.toResponse(null)).isNull();assertThat(manufacturers.toRequest(null)).isNull();
  var appliances=Mappers.getMapper(ApplianceMapper.class);var a=new Appliance();a.setName("unchanged");appliances.applyRequest(null,a);assertThat(a.getName()).isEqualTo("unchanged");assertThat(appliances.toResponse(null)).isNull();assertThat(appliances.toRequest(null)).isNull();
  assertThat(appliances.toResponse(a).getManufacturerId()).isNull();assertThat(appliances.toResponse(a).getManufacturerName()).isNull();
 }
 @Test void incompleteOrdersHaveExplicitAbsentAssociationsAndFalseApproval(){
  var mapper=Mappers.getMapper(OrderMapper.class);assertThat(mapper.toSummary(null)).isNull();assertThat(mapper.toResponse(null,null)).isNull();
  assertThat(mapper.toResponse(null,BigDecimal.ZERO).summary()).isNull();assertThat(mapper.toResponse(new Orders(),null).total()).isNull();
  var order=new Orders();order.setApproved(null);var summary=mapper.toSummary(order);assertThat(summary.approved()).isFalse();assertThat(summary.clientId()).isNull();assertThat(summary.clientName()).isNull();
  assertThat(mapper.toRequest(null)).isNull();assertThat(mapper.toRequestItem(null)).isNull();assertThat(mapper.toItem(null)).isNull();
  assertThat(mapper.toRequest(new OrderResponse(null,null,null)).getItems()).isNull();
  var row=new OrderRow();assertThat(mapper.toItem(row).applianceId()).isNull();assertThat(mapper.toItem(row).applianceName()).isNull();
 }
}
