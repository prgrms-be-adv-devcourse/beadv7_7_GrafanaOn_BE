package shop.dear.common.auth;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRole {

    String ROLE_HEADER = "X-Authenticated-Member-Role";

    String[] roles();
}
