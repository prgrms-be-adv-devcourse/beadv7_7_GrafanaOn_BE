package shop.dear.common.auth;

import java.lang.annotation.*;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {

  String MEMBER_ID_HEADER = "X-Authenticated-Member-Id";
}
