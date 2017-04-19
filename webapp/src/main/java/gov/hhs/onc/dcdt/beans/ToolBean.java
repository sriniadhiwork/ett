package gov.hhs.onc.dcdt.beans;

import org.spockframework.util.Nullable;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NamedBean;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonAutoDetect(getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE, fieldVisibility = Visibility.NONE)
@JsonTypeInfo(use = Id.NAME)
public interface ToolBean extends BeanNameAware, DisposableBean, InitializingBean, NamedBean {
    @Override
    public boolean equals(@Nullable Object obj);

    @Override
    public int hashCode();

    @Override
    public String toString();
}
