package com.appsmith.server.controllers;

import com.appsmith.server.constants.Url;
import com.appsmith.server.domains.Organization;
import com.appsmith.server.domains.UserRole;
import com.appsmith.server.dtos.ResponseDTO;
import com.appsmith.server.services.OrganizationService;
import com.appsmith.server.services.UserOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Url.ORGANIZATION_URL)
public class OrganizationController extends BaseController<OrganizationService, Organization, String> {
    private final UserOrganizationService userOrganizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService, UserOrganizationService userOrganizationService) {
        super(organizationService);
        this.userOrganizationService = userOrganizationService;
    }

    /**
     * This function would be used to fetch all possible user roles at organization level.
     * @return
     */
    @GetMapping("/roles")
    public Mono<ResponseDTO<Map<String, String>>> getUserRolesForOrganization(@RequestParam String organizationId) {
        return service.getUserRolesForOrganization(organizationId)
                .map(permissions -> new ResponseDTO<>(HttpStatus.OK.value(), permissions, null));
    }

    @GetMapping("/{orgId}/members")
    public Mono<ResponseDTO<List<UserRole>>> getUserMembersOfOrganization(@PathVariable String orgId) {
        return service.getOrganizationMembers(orgId)
                .map(users -> new ResponseDTO<>(HttpStatus.OK.value(), users, null));
    }

    @PutMapping("/{orgId}/role")
    public Mono<ResponseDTO<UserRole>> updateRoleForMember(@RequestBody UserRole updatedUserRole, @PathVariable String orgId) {
        return userOrganizationService.updateRoleForMember(orgId, updatedUserRole)
                .map(user -> new ResponseDTO<>(HttpStatus.OK.value(), user, null));
    }

    @PostMapping("/{organizationId}/logo")
    public Mono<ResponseDTO<String>> uploadLogo(@PathVariable String organizationId,
                                                @RequestPart("file") Mono<FilePart> fileMono,
                                                ServerWebExchange exchange) {
        return fileMono
                .zipWhen(part -> { // part.headers().getContentType()
                    return part.content().single();
                })
                .flatMap(tuple -> {
                    final FilePart filePart = tuple.getT1();
                    final DataBuffer buffer = tuple.getT2();
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return service.uploadLogo(organizationId, filePart, bytes);
                })
                .map(url -> new ResponseDTO<>(HttpStatus.OK.value(), url, null));
    }

}
