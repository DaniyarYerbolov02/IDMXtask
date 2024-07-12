@Grab(group='org.postgresql', module='postgresql', version='42.2.27')
@Grab(group='org.slf4j', module='slf4j-api', version='1.7.30')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.30')
import groovy.sql.Sql
import groovy.json.JsonOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IDMXAgent {
    private final Logger log = LoggerFactory.getLogger(IDMXAgent)
    private Sql sql

    IDMXAgent(Map<String, Object> configs) {
        log.info("Initializing connection")
        init(configs)
    }

    void init(Map<String, Object> configs) {
        try {
            String url = configs.url
            String user = configs.username
            String password = configs.password
            log.info("Connecting to database with URL: ${url}")
            sql = Sql.newInstance(url, user, password, 'org.postgresql.Driver')
            log.info("Connection established successfully")
        } catch (Exception e) {
            log.error("Failed to establish connection: ${e.message}", e)
            throw e
        }
    }

    void ending() {
        try {
            sql.close()
            log.info("Connection closed successfully")
        } catch (Exception e) {
            log.error("Failed to close connection: ${e.message}", e)
        }
    }

    boolean isLive() {
        try {
            return sql.connection.isValid(0)
        } catch (Exception e) {
            log.error("Connection is not live: ${e.message}", e)
            return false
        }
    }

    List<Map<String, String>> getRoles() {
        List<Map<String, String>> roles = []
        try {
            log.info("Fetching roles from database")
            sql.eachRow('SELECT * FROM accounts_roles') { row ->
                roles << [id: row.id, name: row.name, note: row.note]
            }
            log.info("Fetched ${roles.size()} roles from the database")
        } catch (Exception e) {
            log.error("Failed to fetch roles: ${e.message}", e)
        }
        return roles
    }

    void writeRolesToJson(List<Map<String, String>> roles, String filePath) {
        try {
            String json = JsonOutput.prettyPrint(JsonOutput.toJson(roles))
            new File(filePath).write(json)
            log.info("Roles written to file successfully")
        } catch (Exception e) {
            log.error("Failed to write roles to file: ${e.message}", e)
        }
    }

    static void main(String[] args) {
        Map<String, Object> configs = [
            url     : 'jdbc:postgresql://localhost:5432/idmx',
            username: 'admin',
            password: 'admin'
        ]

        IDMXAgent agent = new IDMXAgent(configs)
        if (agent.isLive()) {
            List<Map<String, String>> roles = agent.getRoles()
            agent.writeRolesToJson(roles, 'roles.json')
        }
        agent.ending()
    }
}
