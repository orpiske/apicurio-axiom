import { useEffect, useRef, useState } from "react";
import {
    Button,
    Menu,
    MenuContent,
    MenuItem,
    MenuList,
    Popper,
    TextInputGroup,
    TextInputGroupMain,
    TextInputGroupUtilities,
} from "@patternfly/react-core";
import PlusCircleIcon from "@patternfly/react-icons/dist/esm/icons/plus-circle-icon";
import TimesIcon from "@patternfly/react-icons/dist/esm/icons/times-icon";

export interface TypeaheadAddSuggestion {
    value: string;
    style?: React.CSSProperties;
}

interface TypeaheadAddInputProps {
    /** Called when an item is added (from suggestions or freeform text). */
    onAdd: (value: string) => void;
    /** Available suggestions for the typeahead dropdown. */
    suggestions: TypeaheadAddSuggestion[];
    /** Items already in the list, used to filter out duplicates. */
    existingItems?: string[];
    /** Placeholder text for the input. */
    placeholder?: string;
    /** Maximum number of suggestions to show. */
    maxSuggestions?: number;
}

/**
 * A reusable typeahead input for adding items to a list. Shows a plus icon,
 * filters suggestions as the user types, supports ghost-text hint for the
 * first prefix match, and allows freeform entry via Enter.
 */
export function TypeaheadAddInput({
    onAdd,
    suggestions,
    existingItems = [],
    placeholder = "Type to add...",
    maxSuggestions = 12,
}: TypeaheadAddInputProps) {
    const [value, setValue] = useState("");
    const [hint, setHint] = useState("");
    const [isOpen, setIsOpen] = useState(false);
    const [filteredItems, setFilteredItems] = useState<TypeaheadAddSuggestion[]>([]);

    const inputRef = useRef<HTMLInputElement>(null);
    const menuRef = useRef<HTMLDivElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!value.trim()) {
            setFilteredItems([]);
            setIsOpen(false);
            setHint("");
            return;
        }

        const lower = value.toLowerCase();
        const existing = new Set(existingItems.map((t) => t.toLowerCase()));
        const filtered = suggestions.filter(
            (s) =>
                s.value.toLowerCase().includes(lower) &&
                !existing.has(s.value.toLowerCase())
        );

        setFilteredItems(filtered.slice(0, maxSuggestions));
        setIsOpen(filtered.length > 0);

        const startsWithMatch = filtered.find((s) =>
            s.value.toLowerCase().startsWith(lower)
        );
        if (startsWithMatch) {
            setHint(value + startsWithMatch.value.substring(value.length));
        } else {
            setHint("");
        }
    }, [value, existingItems, suggestions, maxSuggestions]);

    useEffect(() => {
        const handleClick = (event: MouseEvent) => {
            if (isOpen && !containerRef.current?.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("click", handleClick);
        return () => document.removeEventListener("click", handleClick);
    }, [isOpen]);

    const doAdd = (item: string) => {
        const trimmed = item.trim();
        if (trimmed) {
            onAdd(trimmed);
            setValue("");
            setHint("");
            setIsOpen(false);
        }
    };

    const onSelect = (
        _event: React.MouseEvent | undefined,
        itemId: string | number | undefined
    ) => {
        doAdd(itemId as string);
        inputRef.current?.focus();
    };

    const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        switch (event.key) {
            case "Enter":
                event.preventDefault();
                if (hint && filteredItems.length === 1) {
                    doAdd(hint);
                } else {
                    doAdd(value);
                }
                break;
            case "Tab":
                if (hint) {
                    event.preventDefault();
                    setValue(hint);
                    setHint("");
                    setIsOpen(false);
                }
                break;
            case "Escape":
                setIsOpen(false);
                break;
            case "ArrowDown":
                if (isOpen) {
                    event.preventDefault();
                    menuRef.current
                        ?.querySelector<HTMLElement>('[role="menuitem"]')
                        ?.focus();
                }
                break;
        }
    };

    return (
        <div ref={containerRef}>
            <Popper
                trigger={
                    <TextInputGroup>
                        <TextInputGroupMain
                            ref={inputRef}
                            icon={<PlusCircleIcon />}
                            placeholder={placeholder}
                            value={value}
                            hint={hint}
                            onChange={(_e, v) => setValue(v)}
                            onKeyDown={handleKeyDown}
                            aria-label="Add item"
                        />
                        {value && (
                            <TextInputGroupUtilities>
                                <Button variant="plain" aria-label="Clear"
                                    onClick={() => { setValue(""); setHint(""); setIsOpen(false); }}>
                                    <TimesIcon />
                                </Button>
                            </TextInputGroupUtilities>
                        )}
                    </TextInputGroup>
                }
                popper={
                    <Menu ref={menuRef} onSelect={onSelect} isScrollable>
                        <MenuContent maxMenuHeight="200px">
                            <MenuList>
                                {filteredItems.map((item) => (
                                    <MenuItem key={item.value} itemId={item.value}
                                        style={item.style}>
                                        {item.value}
                                    </MenuItem>
                                ))}
                            </MenuList>
                        </MenuContent>
                    </Menu>
                }
                isVisible={isOpen}
                enableFlip
                appendTo="inline"
            />
        </div>
    );
}
