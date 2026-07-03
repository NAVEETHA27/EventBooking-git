const e = React.createElement;

function App() {
    const [role, setRole] = React.useState("user");
    const [account, setAccount] = React.useState(null);
    const [events, setEvents] = React.useState([]);
    const [message, setMessage] = React.useState("");
    const [query, setQuery] = React.useState("");

    React.useEffect(() => {
        loadEvents();
    }, []);

    async function api(path, options) {
        const response = await fetch(path, {
            headers: { "Content-Type": "application/json" },
            ...options
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "Request failed");
        }
        return data;
    }

    async function loadEvents(searchText = "") {
        const data = await api("/api/events?q=" + encodeURIComponent(searchText));
        setEvents(data);
        if (data.length === 0) {
            setMessage("No matching events found. Please try another search.");
        }
    }

    function submitAuth(event, mode) {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const payload = Object.fromEntries(form.entries());
        const prefix = role === "user" ? "/api/auth/user/" : "/api/auth/organizer/";
        api(prefix + mode, { method: "POST", body: JSON.stringify(payload) })
            .then(data => {
                setAccount(data);
                setMessage(mode === "register" ? "Registration successful" : "Login successful");
            })
            .catch(error => setMessage(error.message));
    }

    function createEvent(event) {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
        payload.organizerId = String(account.id);
        api("/api/events", { method: "POST", body: JSON.stringify(payload) })
            .then(created => {
                setMessage("Event created successfully");
                setEvents(items => [created, ...items]);
                event.currentTarget.reset();
            })
            .catch(error => setMessage(error.message));
    }

    function book(eventId, quantity) {
        if (!account || account.role !== "USER") {
            setMessage("Please login as user before booking tickets");
            return;
        }
        api("/api/bookings", {
            method: "POST",
            body: JSON.stringify({ userId: String(account.id), eventId: String(eventId), quantity: String(quantity) })
        })
            .then(data => {
                setMessage(data.message + ". Ticket ID: " + data.ticketId);
                loadEvents(query);
            })
            .catch(error => setMessage(error.message));
    }

    return e("div", { className: "shell" },
        e("header", { className: "topbar" },
            e("div", { className: "brand" }, "Online Event Ticket Booking System"),
            e("div", { className: "tabs" },
                e("button", { className: role === "user" ? "" : "secondary", onClick: () => setRole("user") }, "User"),
                e("button", { className: role === "organizer" ? "" : "secondary", onClick: () => setRole("organizer") }, "Organizer")
            )
        ),
        account && e("p", { className: "muted" }, "Signed in as " + account.name + " (" + account.role + ")"),
        message && e("div", { className: "alert" }, message),
        e("section", { className: "grid" },
            e(AuthPanel, { role, onSubmit: submitAuth }),
            account && account.role === "ORGANIZER" && e(EventForm, { onSubmit: createEvent }),
            e(SearchPanel, { query, setQuery, onSearch: loadEvents })
        ),
        e("section", { className: "grid", style: { marginTop: "16px" } },
            events.map(item => e(EventCard, { key: item.id, event: item, onBook: book }))
        )
    );
}

function AuthPanel({ role, onSubmit }) {
    const isUser = role === "user";
    return e("div", { className: "panel" },
        e("h2", null, isUser ? "User Account" : "Organizer Account"),
        e("form", { className: "form", onSubmit: event => onSubmit(event, "register") },
            e("input", { name: isUser ? "name" : "organizerName", placeholder: isUser ? "User Name" : "Organizer Name", required: true }),
            !isUser && e("input", { name: "organizationName", placeholder: "Organization Name", required: true }),
            e("input", { name: "email", type: "email", placeholder: "Email ID", required: true }),
            e("input", { name: "password", type: "password", placeholder: "Password", required: true }),
            e("input", { name: "phone", placeholder: "Phone Number" }),
            e("input", { name: "address", placeholder: "Address" }),
            isUser && e("input", { name: "dateOfBirth", type: "date" }),
            isUser && e("select", { name: "gender" },
                e("option", { value: "" }, "Gender"),
                e("option", { value: "Female" }, "Female"),
                e("option", { value: "Male" }, "Male"),
                e("option", { value: "Other" }, "Other")
            ),
            e("button", null, "Register")
        ),
        e("form", { className: "form", style: { marginTop: "14px" }, onSubmit: event => onSubmit(event, "login") },
            e("input", { name: "email", type: "email", placeholder: "Email ID", required: true }),
            e("input", { name: "password", type: "password", placeholder: "Password", required: true }),
            e("button", { className: "secondary" }, "Login")
        )
    );
}

function EventForm({ onSubmit }) {
    return e("div", { className: "panel" },
        e("h2", null, "Create Event"),
        e("form", { className: "form", onSubmit },
            e("input", { name: "eventName", placeholder: "Event Name", required: true }),
            e("textarea", { name: "description", placeholder: "Event Description" }),
            e("input", { name: "eventDate", type: "date", required: true }),
            e("input", { name: "eventTime", type: "time", required: true }),
            e("input", { name: "eventType", placeholder: "Event Type", required: true }),
            e("input", { name: "venueName", placeholder: "Venue Name" }),
            e("input", { name: "location", placeholder: "Event Location/Address", required: true }),
            e("input", { name: "ticketPrice", type: "number", min: "0", step: "0.01", placeholder: "Ticket Price", required: true }),
            e("input", { name: "availableSeats", type: "number", min: "1", placeholder: "Available Seats", required: true }),
            e("button", null, "Add Event")
        )
    );
}

function SearchPanel({ query, setQuery, onSearch }) {
    return e("div", { className: "panel" },
        e("h2", null, "Search Events"),
        e("form", { className: "form", onSubmit: event => { event.preventDefault(); onSearch(query); } },
            e("input", { value: query, onChange: event => setQuery(event.target.value), placeholder: "Event name, location, type" }),
            e("button", null, "Search")
        )
    );
}

function EventCard({ event, onBook }) {
    const [quantity, setQuantity] = React.useState(1);
    return e("article", { className: "event" },
        e("h3", null, event.eventName),
        e("div", { className: "muted" }, event.eventType + " | " + event.eventDate + " " + event.eventTime),
        e("div", null, event.venueName + " - " + event.location),
        e("div", null, "Price: " + event.ticketPrice + " | Seats: " + event.availableSeats),
        e("input", { type: "number", min: "1", max: event.availableSeats, value: quantity, onChange: item => setQuantity(item.target.value) }),
        e("button", { onClick: () => onBook(event.id, quantity) }, "Book Tickets")
    );
}

ReactDOM.createRoot(document.getElementById("root")).render(e(App));

