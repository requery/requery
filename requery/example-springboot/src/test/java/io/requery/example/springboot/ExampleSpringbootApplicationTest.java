package io.requery.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.requery.example.springboot.entity.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExampleSpringbootApplicationTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;
    private User testUser;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        objectMapper = new ObjectMapper();
        testUser = new User("John", "Smith");
        testUser.setId(1);
    }

    @Test
    public void createUser() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUser);
        postUserAndExpectSame(userJson);
    }

    @Test
    public void findUserById() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUser);
        mockMvc.perform(
                post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson));

        mockMvc.perform(get("/user").param("id", "1")
                .contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void updateUser() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUser);
        postUserAndExpectSame(userJson);
        testUser.setFirstName("Henry");
        userJson = objectMapper.writeValueAsString(testUser);
        postUserAndExpectSame(userJson);
    }

    private void postUserAndExpectSame(String userJson) throws Exception {
        mockMvc.perform(
                post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson)
        ).andExpect(status().isCreated()).andExpect(content().json(userJson));
    }
}
