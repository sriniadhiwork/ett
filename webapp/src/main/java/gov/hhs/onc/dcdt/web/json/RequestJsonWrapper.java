package gov.hhs.onc.dcdt.web.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import gov.hhs.onc.dcdt.beans.ToolBean;
import gov.hhs.onc.dcdt.json.ToolBeanJsonDto;
import gov.hhs.onc.dcdt.web.json.impl.RequestJsonWrapperImpl;

@JsonTypeInfo(
use = JsonTypeInfo.Id.NAME,
include = JsonTypeInfo.As.PROPERTY,
property = "type")
@JsonSubTypes({
@JsonSubTypes.Type(value = RequestJsonWrapperImpl.class, name = "RequestJsonWrapperImpl"),
})public interface RequestJsonWrapper<T extends ToolBean, U extends ToolBeanJsonDto<T>> extends JsonWrapper<T, U> {
}
