# Writing Effective Prompts

## Overview

The quality of your automation results depends heavily on how you write your prompts. This guide provides best practices and examples for writing clear, effective prompts that produce reliable automation.

## Core Principles

### 1. Be Specific and Clear

❌ **Bad**: "Search for something"
✅ **Good**: "Go to google.com and search for 'Krista automation platform'"

❌ **Bad**: "Fill the form"
✅ **Good**: "Fill in the name field with 'John Doe', email field with 'john@example.com', and message field with 'Test message'"

### 2. Include Full URLs

❌ **Bad**: "Go to the login page"
✅ **Good**: "Navigate to https://example.com/login"

❌ **Bad**: "Open the website"
✅ **Good**: "Navigate to https://www.example.com"

### 3. Break Down Complex Tasks

❌ **Bad**: "Login and get all the data from the dashboard"
✅ **Good**:
```
Step 1: "Navigate to https://example.com/login"
Step 2: "Enter 'user@example.com' in the email field and 'password123' in the password field"
Step 3: "Click the 'Login' button"
Step 4: "Wait for the dashboard to load"
Step 5: "Extract all product names and prices from the dashboard table"
```

### 4. Specify What Data to Extract

❌ **Bad**: "Get the information from the page"
✅ **Good**: "Extract the product name, price, description, and availability status from each product card"

### 5. Use Action Verbs

Good action verbs:
- Navigate to
- Click on
- Enter / Type
- Select from dropdown
- Extract / Get
- Wait for
- Verify / Check
- Scroll to
- Download
- Upload

## Prompt Structure

### Basic Structure

```
[Action] + [Target] + [Details]

Examples:
- Click on the "Submit" button
- Enter "john@example.com" in the email field
- Extract the product name from the h1 heading
```

### Multi-Step Structure

```
1. [First Action]
2. [Second Action]
3. [Third Action]
...

Example:
1. Navigate to https://example.com/products
2. Click on the first product in the list
3. Extract the product name, price, and description
4. Take a screenshot of the product page
```

## Common Patterns

### Navigation

```
✅ "Navigate to https://example.com"
✅ "Go to https://example.com/products"
✅ "Open https://example.com in a new tab"
```

### Form Filling

```
✅ "Enter 'John Doe' in the field labeled 'Full Name'"
✅ "Type 'john@example.com' in the email input field"
✅ "Select 'United States' from the country dropdown"
✅ "Check the 'I agree to terms' checkbox"
```

### Clicking Elements

```
✅ "Click the button with text 'Submit'"
✅ "Click on the link that says 'Learn More'"
✅ "Click the first product in the list"
✅ "Click the element with class 'add-to-cart'"
```

### Data Extraction

```
✅ "Extract the text from the h1 heading"
✅ "Get all product names from the page"
✅ "Extract the price, name, and description from each product card"
✅ "Get the value of the 'data-id' attribute from the selected element"
```

### Waiting and Verification

```
✅ "Wait for the page to finish loading"
✅ "Wait for the element with text 'Success' to appear"
✅ "Verify that the login button is visible"
✅ "Check if the error message is displayed"
```

## Advanced Techniques

### Conditional Logic

```
"If the login button is visible, click it. Otherwise, check if we're already logged in by looking for the user menu."
```

### Loops and Iteration

```
"For each product card on the page, extract the product name and price, then store them in a list."
```

### Error Handling

```
"Try to click the submit button. If it's disabled, fill in any required fields that are empty, then try again."
```

## Context-Specific Tips

### For Single Command Mode

- Include all necessary context in one prompt
- Don't reference previous actions (no state)
- Be complete and self-contained

```
✅ Good Single Command Prompt:
"Navigate to https://example.com/products, click on the first product, extract the product name and price, and take a screenshot."
```

### For Interactive Session Mode

- Build on previous steps
- Reference current page state
- Use incremental instructions

```
✅ Good Session Step 1:
"Navigate to https://example.com/login"

✅ Good Session Step 2:
"Enter credentials and click login"
(Assumes we're on the login page from Step 1)

✅ Good Session Step 3:
"Extract the user's name from the dashboard"
(Assumes we're logged in from Step 2)
```

## Common Mistakes to Avoid

### 1. Vague Instructions

❌ "Get the data"
✅ "Extract the product name, price, and stock status from the product details section"

### 2. Missing URLs

❌ "Go to the homepage"
✅ "Navigate to https://www.example.com"

### 3. Ambiguous Selectors

❌ "Click the button"
✅ "Click the blue 'Submit' button at the bottom of the form"

### 4. Assuming Context (in Single Command Mode)

❌ "Click the next button" (What page are we on?)
✅ "Navigate to https://example.com/products, then click the 'Next Page' button"

### 5. Too Many Steps in One Prompt

❌ "Login, navigate to products, filter by category, sort by price, extract all data, download report, logout"
✅ Break into multiple steps (use Interactive Session Mode)

## Examples by Use Case

### Web Scraping

```
"Navigate to https://example.com/products
Extract the following information from each product:
- Product name (from the h2 heading)
- Price (from the element with class 'price')
- Rating (from the star rating component)
- Availability (from the stock status text)
Return the data as a structured list."
```

### Form Automation

```
"Navigate to https://example.com/contact
Fill in the form with the following information:
- Name: John Doe
- Email: john@example.com
- Phone: 555-1234
- Message: This is a test message
Then click the 'Send Message' button
Wait for the confirmation message to appear"
```

### Authentication

```
Step 1: "Navigate to https://example.com/login"
Step 2: "Enter 'user@example.com' in the email field"
Step 3: "Enter 'SecurePassword123' in the password field"
Step 4: "Click the 'Sign In' button"
Step 5: "Wait for the dashboard page to load and verify the user menu is visible"
```

## Testing Your Prompts

1. **Start Simple**: Test with basic prompts first
2. **Add Complexity Gradually**: Build up to complex workflows
3. **Use Validation Steps**: Verify each step succeeded
4. **Review Screenshots**: Check visual output for accuracy
5. **Iterate**: Refine prompts based on results

## Next Steps

- Practice with [Single Command Mode](/pages/SingleCommandMode.md)
- Learn about [Validation Steps](/pages/ValidationSteps.md)
- Review [Automation Modes](/pages/AutomationModes.md)

---

> **💡 Tip**: When in doubt, be more specific rather than less. The AI can handle detailed instructions better than vague ones.

